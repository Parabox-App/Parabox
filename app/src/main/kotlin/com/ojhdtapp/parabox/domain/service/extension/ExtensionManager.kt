package com.ojhdtapp.parabox.domain.service.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.ConnectionInfoType
import com.ojhdtapp.parabox.domain.built_in.BuiltInExtensionUtil
import com.ojhdtapp.parabox.domain.model.BuiltInConnection
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.ExternalConnection
import com.ojhdtapp.parabox.domain.repository.ConnectionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionTransport
import com.ojhdtapp.parabox.protocol.v2.ExecuteConfigActionRequest
import com.ojhdtapp.parabox.protocol.v2.GetConfigSchemaRequest
import com.ojhdtapp.parabox.protocol.v2.InitStepRequest
import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.parabox.protocol.v2.ValidateConfigRequest
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExtensionManager @Inject constructor(
    @ApplicationContext val context: Context,
    val mainRepository: MainRepository,
    val connectionInfoRepository: ConnectionInfoRepository,
    private val transport: ExtensionTransport,
    private val trustStore: ExtensionTrustStore,
    private val discovery: ExtensionDiscovery,
) : DefaultLifecycleObserver {

    private var managerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var attachedLifecycle: Lifecycle? = null

    // Discovered external extensions (combined with built-in extensions in extensionFlow)
    private val _externalExtensionFlow = MutableStateFlow(emptyList<Extension>())
    val extensionFlow
        get() = _externalExtensionFlow.asStateFlow().map { BuiltInExtensionUtil.getAllExtension() + it }

    // Active / pending / failed connections
    private val _connectionFlow = MutableStateFlow(emptyList<Connection>())
    val connectionFlow = _connectionFlow.asStateFlow()

    // Onboarding wizard state
    private var initializingExtension: Extension.Success? = null
    private var wizardSessionId: String = ""
    private var wizardExtra: JSONObject = JSONObject()
    private val _initActionStateFlow = MutableStateFlow<SettingPageState.InitActionState?>(null)
    val initActionStateFlow = _initActionStateFlow.asStateFlow()

    private var awaitInitActionResJob: Job? = null

    private val extensionPackageActionReceiver: ExtensionPackageActionReceiver by lazy {
        ExtensionPackageActionReceiver()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        attachedLifecycle = owner.lifecycle
        managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        BuiltInExtensionUtil.ensureStarted()
        extensionPackageActionReceiver.register()
        manageLifecycleOfExtensions(owner.lifecycle)
        reloadExtension()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        extensionPackageActionReceiver.safeUnregister()
        _connectionFlow.value.filterIsInstance<Connection.ConnectionSuccess>().forEach { conn ->
            attachedLifecycle?.removeObserver(conn)
            conn.closeSession()
        }
        transport.shutdownAll()
        BuiltInExtensionUtil.shutdown()
        managerScope.cancel()
        attachedLifecycle = null
        super.onDestroy(owner)
    }

    fun dispatchHostLifecycle(event: LifecycleEventProto) {
        _connectionFlow.value
            .filterIsInstance<Connection.ConnectionSuccess>()
            .forEach { conn ->
                conn.sendLifecycleEvent(event)
            }
    }

    fun reloadExtension() {
        managerScope.launch {
            val external = discovery.scanAllExternalExtensions()
            _externalExtensionFlow.value = external
        }
    }

    fun trustExtension(packageName: String, signatureSha256: String) {
        managerScope.launch {
            trustStore.trust(packageName, signatureSha256)
            val refreshed = discovery.scanExternalPackage(packageName)
            replaceExternalExtensionsForPackage(packageName, refreshed)
            // Retry any failed connections belonging to this newly trusted package
            val retryList = _connectionFlow.value
                .filterIsInstance<Connection.ConnectionFail.ExtendConnectionFail>()
                .filter { it.pkg == packageName }
                .map { Connection.ConnectionPending.ExtendConnectionPending(it.toExtensionInfo()) }
            if (retryList.isNotEmpty()) {
                updateConnections(retryList)
            }
        }
    }

    fun revokeExtensionTrust(packageName: String) {
        managerScope.launch {
            trustStore.revoke(packageName)
            transport.invalidatePackage(packageName)
            failActiveConnectionsForPackage(packageName, "扩展签名授权已撤销")
            val refreshed = discovery.scanExternalPackage(packageName)
            replaceExternalExtensionsForPackage(packageName, refreshed)
        }
    }

    suspend fun initNewExtensionConnection(extension: Extension.Success) {
        if (initializingExtension?.key != extension.key) {
            resetInitAction(false)
        }
        initializingExtension = extension
        wizardSessionId = UUID.randomUUID().toString()
        wizardExtra = JSONObject()

        val initialSteps = try {
            val stub = createUnaryStub(extension.endpoint, WIZARD_RPC_TIMEOUT_SECONDS)
            val resp = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId(wizardSessionId)
                    .setExtensionKey(extension.key)
                    .setCurrentStepIndex(-1)
                    .setAccumulatedExtraJson(wizardExtra.toString())
                    .build()
            )
            if (resp.updatedExtraJson.isNotBlank()) {
                wizardExtra = runCatching { JSONObject(resp.updatedExtraJson) }.getOrDefault(wizardExtra)
            }
            resp.stepsList.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "initNewExtensionConnection failed for ${extension.key}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "无法加载扩展配置步骤：${e.message}", Toast.LENGTH_SHORT).show()
            }
            emptyList()
        }

        _initActionStateFlow.value = SettingPageState.InitActionState(
            name = extension.name,
            actionList = listOf(aliasAction) + initialSteps,
            currentIndex = 0,
            isLoading = false,
        )
    }

    suspend fun revertInitAction() {
        awaitInitActionResJob?.cancel()
        val current = _initActionStateFlow.value ?: return
        if (initializingExtension != null && current.currentIndex > 0) {
            _initActionStateFlow.value = current.copy(
                currentIndex = current.currentIndex - 1,
                isLoading = false,
            )
        }
    }

    suspend fun submitInitActionResult(result: Any) {
        val ext = initializingExtension
        val state = _initActionStateFlow.value
        if (ext == null || state == null) {
            Log.e(TAG, "submitInitActionResult: initializingExtension or state is null")
            return
        }
        awaitInitActionResJob?.cancel()
        val currentIndex = state.currentIndex
        val action = state.actionList.getOrNull(currentIndex) ?: return

        awaitInitActionResJob = managerScope.launch {
            if (currentIndex == 0 && action.key == ALIAS_KEY) {
                val alias = result.toString().trim()
                if (alias.isEmpty()) {
                    updateStepError(currentIndex, "别名不能为空")
                } else {
                    wizardExtra.put(ALIAS_KEY, alias)
                    clearStepErrorAndAdvance(currentIndex, currentIndex + 1, state.actionList)
                }
                return@launch
            }

            _initActionStateFlow.update { it?.copy(isLoading = true) }
            try {
                val stub = createUnaryStub(ext.endpoint, WIZARD_RPC_TIMEOUT_SECONDS)
                val remoteIndex = currentIndex - 1
                val resp = stub.nextInitStep(
                    InitStepRequest.newBuilder()
                        .setWizardSessionId(wizardSessionId)
                        .setExtensionKey(ext.key)
                        .setCurrentStepIndex(remoteIndex)
                        .setSubmittedStepKey(action.key)
                        .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(result))
                        .setAccumulatedExtraJson(wizardExtra.toString())
                        .build()
                )
                if (resp.updatedExtraJson.isNotBlank()) {
                    val updated = runCatching { JSONObject(resp.updatedExtraJson) }.getOrNull()
                    if (updated != null) {
                        val savedAlias = wizardExtra.optString(ALIAS_KEY)
                        wizardExtra = updated
                        if (savedAlias.isNotBlank() && !wizardExtra.has(ALIAS_KEY)) {
                            wizardExtra.put(ALIAS_KEY, savedAlias)
                        }
                    }
                }
                val newActionList = listOf(aliasAction) + resp.stepsList.map { it.toDomain() }
                if (resp.validationError.isNotEmpty()) {
                    val updatedList = newActionList.toMutableList()
                    if (currentIndex in updatedList.indices) {
                        updatedList[currentIndex] = updatedList[currentIndex].withError(resp.validationError)
                    }
                    _initActionStateFlow.value = state.copy(
                        actionList = updatedList,
                        currentIndex = currentIndex,
                        isLoading = false,
                    )
                } else {
                    val nextIdx = if (resp.completed) {
                        newActionList.size
                    } else {
                        (resp.nextStepIndex + 1).coerceIn(1, newActionList.size)
                    }
                    _initActionStateFlow.value = state.copy(
                        actionList = newActionList,
                        currentIndex = nextIdx,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitInitActionResult RPC error", e)
                updateStepError(currentIndex, e.message ?: "扩展服务校验失败")
            }
        }
    }

    private fun updateStepError(stepIndex: Int, errorMessage: String) {
        _initActionStateFlow.update { current ->
            current ?: return@update null
            val list = current.actionList.toMutableList()
            if (stepIndex in list.indices) {
                list[stepIndex] = list[stepIndex].withError(errorMessage)
            }
            current.copy(actionList = list, isLoading = false)
        }
    }

    private fun clearStepErrorAndAdvance(stepIndex: Int, nextIndex: Int, list: List<ParaboxInitAction>) {
        val updated = list.toMutableList()
        if (stepIndex in updated.indices) {
            updated[stepIndex] = updated[stepIndex].withError("")
        }
        _initActionStateFlow.update { current ->
            current?.copy(actionList = updated, currentIndex = nextIndex, isLoading = false)
        }
    }

    suspend fun resetInitAction(isDone: Boolean) {
        val ext = initializingExtension
        if (isDone && ext != null) {
            val extraCopy = JSONObject(wizardExtra.toString())
            val connectionInfo = when (ext) {
                is Extension.Success.BuiltIn -> ConnectionInfo(
                    alias = extraCopy.optString(ALIAS_KEY, ext.key).ifBlank { ext.key },
                    name = ext.name,
                    type = ConnectionInfoType.BuiltIn,
                    extra = extraCopy,
                    pkg = "",
                    connectionClassName = "",
                    version = "",
                    versionCode = 0,
                    key = ext.key,
                )

                is Extension.Success.External -> ConnectionInfo(
                    alias = extraCopy.optString(ALIAS_KEY, ext.key).ifBlank { ext.key },
                    name = ext.name,
                    type = ConnectionInfoType.Extend,
                    extra = extraCopy,
                    pkg = ext.pkg,
                    connectionClassName = ext.serviceClassName,
                    version = ext.version,
                    versionCode = ext.versionCode,
                    key = ext.key,
                )
            }
            withContext(Dispatchers.IO) {
                connectionInfoRepository.insertConnectionInfo(connectionInfo.toConnectionInfoEntity())
            }
        }

        initializingExtension = null
        wizardSessionId = ""
        wizardExtra = JSONObject()
        awaitInitActionResJob?.cancel()
        awaitInitActionResJob = null
    }

    suspend fun getConnectionConfig(connection: Connection): Resource<List<ParaboxConfigItem>> {
        val endpoint = resolveEndpointForConnection(connection)
            ?: return Resource.Error("未找到可用的扩展服务")
        return try {
            val stub = createUnaryStub(endpoint)
            val resp = stub.getConfigSchema(
                GetConfigSchemaRequest.newBuilder()
                    .setExtensionKey(connection.key)
                    .setCurrentExtraJson(connection.extra.toString())
                    .build()
            )
            Resource.Success(resp.itemsList.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "getConnectionConfig failed", e)
            Resource.Error(e.message ?: "获取扩展配置失败")
        }
    }

    suspend fun validateConnectionConfig(
        connection: Connection,
        configKey: String,
        value: Any?,
        currentExtra: JSONObject,
    ): ParaboxInitActionResult {
        val endpoint = resolveEndpointForConnection(connection)
            ?: return ParaboxInitActionResult.Error("未找到可用的扩展服务")
        return try {
            val stub = createUnaryStub(endpoint)
            val resp = stub.validateConfig(
                ValidateConfigRequest.newBuilder()
                    .setExtensionKey(connection.key)
                    .setConfigKey(configKey)
                    .setValue(ParaboxProtoMapper.toConfigValueProto(value))
                    .setCurrentExtraJson(currentExtra.toString())
                    .build()
            )
            if (resp.valid) {
                ParaboxInitActionResult.Done
            } else {
                ParaboxInitActionResult.Error(resp.errorMessage.ifBlank { "输入值无效" })
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateConnectionConfig failed", e)
            ParaboxInitActionResult.Error(e.message ?: "校验请求失败")
        }
    }

    suspend fun executeConnectionConfigAction(
        connection: Connection,
        actionKey: String,
        currentExtra: JSONObject,
    ): ParaboxResult {
        val endpoint = resolveEndpointForConnection(connection)
            ?: return ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, "未找到可用的扩展服务")
        return try {
            val stub = createUnaryStub(endpoint, WIZARD_RPC_TIMEOUT_SECONDS)
            stub.executeConfigAction(
                ExecuteConfigActionRequest.newBuilder()
                    .setExtensionKey(connection.key)
                    .setSessionId("conn-${connection.connectionId}")
                    .setActionKey(actionKey)
                    .setCurrentExtraJson(currentExtra.toString())
                    .build()
            ).toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "executeConnectionConfigAction failed", e)
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: "操作执行失败")
        }
    }

    private fun safeRemoveLifecycleObserver(observer: LifecycleObserver) {
        val lifecycle = attachedLifecycle ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            lifecycle.removeObserver(observer)
        } else {
            Handler(Looper.getMainLooper()).post {
                lifecycle.removeObserver(observer)
            }
        }
    }

    fun restartConnection(connectionId: Long, needReloadExtra: Boolean) {
        _connectionFlow.value.find { it.connectionId == connectionId }?.also { conn ->
            when (conn) {
                is Connection.ConnectionSuccess -> {
                    safeRemoveLifecycleObserver(conn)
                    conn.closeSession()
                    updateConnections(listOf(conn.toPending(needReloadExtra = needReloadExtra)))
                }

                is Connection.ConnectionFail -> {
                    val pending = when (conn) {
                        is Connection.ConnectionFail.BuiltInConnectionFail ->
                            Connection.ConnectionPending.BuiltInConnectionPending(conn.toExtensionInfo(), needReloadExtra)
                        is Connection.ConnectionFail.ExtendConnectionFail ->
                            Connection.ConnectionPending.ExtendConnectionPending(conn.toExtensionInfo(), needReloadExtra)
                    }
                    updateConnections(listOf(pending))
                }

                is Connection.ConnectionPending -> Unit
            }
        }
    }

    private fun createAndTryAppendConnection(connectionInfo: ConnectionInfo) {
        val pending: Connection = when (connectionInfo.type) {
            ConnectionInfoType.BuiltIn -> Connection.ConnectionPending.BuiltInConnectionPending(connectionInfo)
            ConnectionInfoType.Extend -> Connection.ConnectionPending.ExtendConnectionPending(connectionInfo)
        }
        _connectionFlow.update { current ->
            if (current.any { it.connectionId == connectionInfo.connectionId }) current else current + pending
        }
    }

    private fun updateConnections(newConnectionList: List<Connection>) {
        if (newConnectionList.isEmpty()) return
        _connectionFlow.update { current ->
            current.map { existing ->
                newConnectionList.find { it.connectionId == existing.connectionId } ?: existing
            }
        }
    }

    private fun removeConnection(connectionId: Long) {
        _connectionFlow.update { current ->
            current.filter { it.connectionId != connectionId }
        }
    }

    private fun manageLifecycleOfExtensions(lifecycle: Lifecycle) {
        connectionInfoRepository.getConnectionInfoList()
            .filter { it is Resource.Success && it.data != null }
            .map { it.data.orEmpty() }
            .combine(connectionFlow) { dbList, runningList ->
                val pendingConnections = runningList.filterIsInstance<Connection.ConnectionPending>()
                if (pendingConnections.isNotEmpty()) {
                    val transitioned = pendingConnections.map { pending ->
                        startPendingConnection(lifecycle, pending, dbList)
                    }
                    updateConnections(transitioned)
                }

                val runningIds = runningList.map { it.connectionId }.toSet()
                dbList.filterNot { it.connectionId in runningIds }.forEach { info ->
                    createAndTryAppendConnection(info)
                }

                val dbIds = dbList.map { it.connectionId }.toSet()
                runningList.filterNot { it.connectionId in dbIds }.forEach { removed ->
                    (removed as? Connection.ConnectionSuccess)?.let { success ->
                        lifecycle.removeObserver(success)
                        success.closeSession()
                    }
                    removeConnection(removed.connectionId)
                }
            }
            .launchIn(lifecycle.coroutineScope)
    }

    private suspend fun startPendingConnection(
        lifecycle: Lifecycle,
        pending: Connection.ConnectionPending,
        dbList: List<ConnectionInfo>,
    ): Connection {
        val updatedExtra = if (pending.needReloadExtra) {
            dbList.find { it.connectionId == pending.connectionId }?.extra
        } else {
            null
        }
        val effectiveExtra = updatedExtra ?: pending.extra

        val endpoint = when (pending) {
            is Connection.ConnectionPending.BuiltInConnectionPending -> {
                BuiltInExtensionUtil.getEndpointByKey(pending.key)
                    ?: return pending.toFail("内置扩展不存在 (${pending.key})")
            }

            is Connection.ConnectionPending.ExtendConnectionPending -> {
                val installedSig = SignatureVerifier.getPackageSignatureSha256(context, pending.pkg)
                    ?: return pending.toFail("扩展包未安装 (${pending.pkg})")
                val trustedSig = trustStore.getTrustedSignature(pending.pkg)
                    ?: return pending.toFail("扩展尚未授权签名")
                if (!trustedSig.equals(installedSig, ignoreCase = true)) {
                    return pending.toFail("扩展签名已变更，请重新授权")
                }
                val serviceCls = pending.serviceClassName.ifBlank {
                    return pending.toFail("缺少扩展服务类名")
                }
                ExtensionEndpoint.BinderService(
                    packageName = pending.pkg,
                    serviceClassName = serviceCls,
                    extensionKey = pending.key,
                    expectedSignatureSha256 = trustedSig,
                )
            }
        }

        lateinit var connectionSuccess: Connection.ConnectionSuccess
        val inboundHandler = object : SessionInboundHandler {
            override suspend fun onReceiveMessage(message: ReceiveMessage, isBackfill: Boolean): ParaboxResult {
                return mainRepository.receiveMessage(
                    msg = message,
                    ext = connectionSuccess,
                    isBackfill = isBackfill,
                )
            }

            override suspend fun onRecallMessage(messageUuid: String): ParaboxResult {
                return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            }
        }

        val sessionClient = ExtensionSessionClient(
            sessionId = "conn-${pending.connectionId}",
            endpoint = endpoint,
            extra = effectiveExtra,
            hostPackageName = context.packageName,
            transport = transport,
            inboundHandler = inboundHandler,
            uriPermissionGranter = { targetPkg, uri ->
                context.grantUriPermission(targetPkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        connectionSuccess = pending.toSuccess(sessionClient, updatedExtra)
        lifecycle.addObserver(connectionSuccess)
        sessionClient.start()
        return connectionSuccess
    }

    private suspend fun resolveEndpointForConnection(connection: Connection): ExtensionEndpoint? {
        return when (connection) {
            is Connection.ConnectionSuccess -> connection.sessionClient.endpoint
            is BuiltInConnection -> BuiltInExtensionUtil.getEndpointByKey(connection.key)
            is ExternalConnection -> {
                val ext = extensionFlow.firstOrNull()
                    ?.filterIsInstance<Extension.Success.External>()
                    ?.find { it.pkg == connection.pkg && it.key == connection.key }
                ext?.endpoint
            }
            else -> null
        }
    }

    private fun createUnaryStub(
        endpoint: ExtensionEndpoint,
        timeoutSeconds: Long = RPC_TIMEOUT_SECONDS,
    ): ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub {
        val channel = transport.acquireChannel(endpoint)
        return ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)
            .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
    }

    private fun replaceExternalExtensionsForPackage(packageName: String, replacements: List<Extension>) {
        _externalExtensionFlow.update { current ->
            current.filterNot { ext ->
                when (ext) {
                    is Extension.Success.External -> ext.pkg == packageName
                    is Extension.Untrusted -> ext.pkg == packageName
                    is Extension.Error -> ext.key.startsWith("$packageName:")
                    is Extension.Success.BuiltIn -> false
                }
            } + replacements
        }
    }

    private fun failActiveConnectionsForPackage(packageName: String, reason: String) {
        val affected = _connectionFlow.value
            .filterIsInstance<Connection.ConnectionSuccess.ExtendConnectionSuccess>()
            .filter { it.pkg == packageName }
            .map { conn ->
                safeRemoveLifecycleObserver(conn)
                conn.closeSession()
                Connection.ConnectionFail.ExtendConnectionFail(conn.toExtensionInfo(), reason)
            }
        if (affected.isNotEmpty()) {
            updateConnections(affected)
        }
    }

    inner class ExtensionPackageActionReceiver : BroadcastReceiver() {
        fun register() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(this, packageReceiverFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(this, packageReceiverFilter)
            }
        }

        fun safeUnregister() {
            try {
                context.unregisterReceiver(this)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister package receiver", e)
            }
        }

        override fun onReceive(ct: Context?, intent: Intent?) {
            val packageName = intent?.data?.encodedSchemeSpecificPart ?: return
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (isReplacing) return
                    managerScope.launch {
                        val scanned = discovery.scanExternalPackage(packageName)
                        if (scanned.isNotEmpty()) {
                            replaceExternalExtensionsForPackage(packageName, scanned)
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    managerScope.launch {
                        transport.invalidatePackage(packageName)
                        val scanned = discovery.scanExternalPackage(packageName)
                        replaceExternalExtensionsForPackage(packageName, scanned)

                        val installedSig = SignatureVerifier.getPackageSignatureSha256(context, packageName)
                        val signatureValid = installedSig != null && trustStore.isTrusted(packageName, installedSig)
                        if (signatureValid) {
                            // Preserve existing ConnectionInfo and smoothly restart affected connections
                            _connectionFlow.value
                                .filter { it is ExternalConnection && it.pkg == packageName }
                                .forEach { conn ->
                                    restartConnection(conn.connectionId, needReloadExtra = false)
                                }
                        } else {
                            failActiveConnectionsForPackage(packageName, "更新后签名不匹配，请重新授权")
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (isReplacing) return
                    managerScope.launch {
                        transport.invalidatePackage(packageName)
                        replaceExternalExtensionsForPackage(packageName, emptyList())
                        connectionInfoRepository.deleteConnectionInfoByPackageName(packageName)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ExtensionManager"
        private const val ALIAS_KEY = "alias"
        private const val RPC_TIMEOUT_SECONDS = 12L
        private const val WIZARD_RPC_TIMEOUT_SECONDS = 45L

        private val aliasAction = ParaboxInitAction.TextInputAction(
            key = ALIAS_KEY,
            title = "输入别名",
            errMsg = "",
            description = "别名将用于区分同一扩展提供的不同连接。",
            label = "别名",
        )

        private val packageReceiverFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
    }
}
