package com.ojhdtapp.parabox.domain.service.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.optStringOrNull
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.ConnectionInfoType
import com.ojhdtapp.parabox.domain.built_in.BuiltInExtensionUtil
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ConnectionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.paraboxdevelopmentkit.BuildConfig
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
import java.util.concurrent.CancellationException

class ExtensionManager(
    val context: Context,
    val mainRepository: MainRepository,
    val connectionInfoRepository: ConnectionInfoRepository
) : DefaultLifecycleObserver {
    // Extension
    private val _extensionFlow = MutableStateFlow(emptyList<Extension>())
    val extensionFlow get() = _extensionFlow.asStateFlow().map { it + BuiltInExtensionUtil.getAllExtension() }

    // Connection
    private val _connectionFlow = MutableStateFlow(emptyList<Connection>())
    val connectionFlow = _connectionFlow.asStateFlow()

    // Init
    private var initializingExtension : Extension.Success? = null
    private val _initActionStateFlow = MutableStateFlow<SettingPageState.InitActionState?>(null)
    val initActionStateFlow = _initActionStateFlow.asStateFlow()

    private var getInitActionJob: Job? = null
    private var awaitInitActionResJob: Job? = null

    // Package Broadcast Receiver
    private val extensionPackageActionReceiver: ExtensionPackageActionReceiver by lazy {
        ExtensionPackageActionReceiver()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        manageLifecycleOfExtensions(owner.lifecycle)
        extensionPackageActionReceiver.register()
        // scan on service first launch
        _extensionFlow.value = ExtensionLoader.scanInstalledApp(context)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        extensionPackageActionReceiver.safeUnregister()
        super.onDestroy(owner)
    }

    fun reloadExtension() {
        _extensionFlow.value = ExtensionLoader.scanInstalledApp(context)
    }

    suspend fun initNewExtensionConnection(extension: Extension.Success) {
        if (initializingExtension?.key != extension.key) {
            resetInitAction(false)
        }
        initializingExtension = extension
        val initActions = try {
            initializingExtension!!.initHandler.getInitAction(emptyList(), 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "发生致命错误，请检查扩展版本", Toast.LENGTH_SHORT).show()
            null
        } catch (e : AbstractMethodError) {
            e.printStackTrace()
            Toast.makeText(context, "发生致命错误，请检查扩展版本", Toast.LENGTH_SHORT).show()
            null
        } ?: emptyList()
        _initActionStateFlow.value =
            SettingPageState.InitActionState(
                name = extension.name,
                actionList = withContext(Dispatchers.IO) {
                    listOf(aliasAction) + initActions
                },
                currentIndex = 0
            )
    }

    suspend fun revertInitAction() {
        awaitInitActionResJob?.cancel()
        if (initializingExtension != null && initActionStateFlow.value != null) {
            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                currentIndex = initActionStateFlow.value!!.currentIndex - 1
            )
        }
    }

    suspend fun submitInitActionResult(result: Any) {
        if (initializingExtension == null || initActionStateFlow.value == null) {
            Log.e("parabox", "submitInitActionResult: packageInfo or initHandler is null")
            return
        }
        awaitInitActionResJob?.cancel()
        coroutineScope {
            val currentActionIndex = initActionStateFlow.value!!.currentIndex
            val action = initActionStateFlow.value!!.actionList.getOrNull(currentActionIndex) ?: return@coroutineScope
            awaitInitActionResJob = launch(Dispatchers.IO) {
                when (action) {
                    is ParaboxInitAction.InfoAction -> {
                        val res = action.onResult()
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initializingExtension!!.initHandler.data.put(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = (res as ParaboxInitActionResult.Error).message
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is ParaboxInitAction.TextInputAction -> {
                        val res = action.onResult(result as String)
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initializingExtension!!.initHandler.data.put(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = (res as ParaboxInitActionResult.Error).message
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is ParaboxInitAction.SelectAction -> {
                        val res = action.onResult(result as Int)
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initializingExtension!!.initHandler.data.put(action.key, action.options[result])
                            increaseInitActionStep()
                        } else {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = (res as ParaboxInitActionResult.Error).message
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is ParaboxInitAction.TextInputWithImageAction -> {
                        val res = action.onResult(result as String)
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initializingExtension!!.initHandler.data.put(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = (res as ParaboxInitActionResult.Error).message
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is ParaboxInitAction.SwitchAction -> {
                        val res = action.onResult(result as Boolean)
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initializingExtension!!.initHandler.data.put(action.key, result as Boolean)
                            increaseInitActionStep()
                        } else {
                            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                                actionList = initActionStateFlow.value!!.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = (res as ParaboxInitActionResult.Error).message
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun resetInitAction(isDone: Boolean) {
        if (isDone && initializingExtension != null) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    val connectionInfo = when(initializingExtension) {
                        is Extension.Success.BuiltIn -> {
                            val initHandler = (initializingExtension as Extension.Success).initHandler
                            ConnectionInfo(
                                alias = initHandler.data.optString(
                                    ALIAS_KEY, initializingExtension!!.key
                                ),
                                name = initializingExtension!!.name,
                                type = ConnectionInfoType.BuiltIn,
                                extra = initHandler.data,
                                pkg = "",
                                connectionClassName = "",
                                version = "",
                                versionCode = 0,
                                key = initializingExtension!!.key
                            )
                        }
                        is Extension.Success.External -> {
                            val initHandler = (initializingExtension as Extension.Success).initHandler
                            ConnectionInfo(
                                alias = initHandler.data.optString(
                                    ALIAS_KEY, initializingExtension!!.key
                                ),
                                name = initializingExtension!!.name,
                                type = ConnectionInfoType.BuiltIn,
                                extra = initHandler.data,
                                pkg = (initializingExtension as Extension.Success.External).pkg,
                                connectionClassName = (initializingExtension as Extension.Success.External).connectionClassName,
                                version = (initializingExtension as Extension.Success.External).version,
                                versionCode = (initializingExtension as Extension.Success.External).versionCode,
                                key = initializingExtension!!.key
                            )
                        }
                        else -> null
                    }
                    connectionInfo?.toConnectionInfoEntity()?.let {
                        connectionInfoRepository.insertConnectionInfo(it)
                    }
                }
            }

        }

        initializingExtension = null
        getInitActionJob?.cancel()
        getInitActionJob = null
        awaitInitActionResJob?.cancel()
        awaitInitActionResJob = null
    }

    private suspend fun increaseInitActionStep() {
        if (initializingExtension != null && initActionStateFlow.value != null) {
            val initActions = try {
                initializingExtension!!.initHandler.getInitAction(emptyList(), 0)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: emptyList()
            _initActionStateFlow.value = initActionStateFlow.value!!.copy(
                actionList = withContext(Dispatchers.IO) {
                    listOf(aliasAction) + initActions
                },
                currentIndex = initActionStateFlow.value!!.currentIndex + 1
            )
        }
    }

    fun createAndTryAppendConnection(connectionInfo: ConnectionInfo) {
        val extension = when (connectionInfo.type) {
            ConnectionInfoType.BuiltIn -> {
                createBuiltInConnection(connectionInfo)
            }

            ConnectionInfoType.Extend -> {
                ExtensionLoader.createExternalConnection(context, connectionInfo)
            }
        }
        _connectionFlow.update {
            it + extension
        }
        Log.d("parabox", "append extension=${_connectionFlow.value}")
    }

    private fun createBuiltInConnection(connectionInfo: ConnectionInfo): Connection {
        return try {
            val ext = BuiltInExtensionUtil.getConnectionByKey(connectionInfo.key)
            if (ext != null) {
                Connection.ConnectionPending.BuiltInConnectionPending(
                    connectionInfo, ext
                )
            } else {
                Connection.ConnectionFail.BuiltInConnectionFail(connectionInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Connection.ConnectionFail.BuiltInConnectionFail(connectionInfo)
        }
    }

    // replace the old extension with the new one if the extensionId is the same
    fun updateConnections(newConnectionList: List<Connection>) {
        _connectionFlow.update {
            it.toMutableList().apply {
                replaceAll {
                    newConnectionList.find { newConnection -> it.connectionId == newConnection.connectionId } ?: it
                }
            }
        }
    }

    fun removeConnection(connectionId: Long) {
        _connectionFlow.update {
            it.filter { it.connectionId != connectionId }
        }
    }

    // Only ExtensionSuccess, no Context and bridge changed
    fun restartConnection(connectionId: Long, needReloadExtra: Boolean) {
        _connectionFlow.value.find { it.connectionId == connectionId }?.also {
            if (it is Connection.ConnectionSuccess) {
                try {
                    it.realConnection.updateStatus(ParaboxConnectionStatus.Pending)
                    it.job.cancel(CancellationException("restart"))
                    it.realConnection.onPause()
                    it.realConnection.onStop()
                    it.realConnection.onDestroy()
                    updateConnections(listOf(it.toPending(needReloadExtra = needReloadExtra)))
                } catch (e: Exception) {
                    it.realConnection.updateStatus(ParaboxConnectionStatus.Error(e.message ?: "restart error"))
                    Log.e("parabox", "restartExtension error", e)
                }
            }
        }
    }

    suspend fun getConnectionConfig(connection: Connection) : Resource<List<ParaboxConfigItem>> {
        val extension = extensionFlow.firstOrNull()?.find { connection.key == it.key } as? Extension.Success
        if (extension != null) {
            val configItems = extension.initHandler.getConfig()
            return Resource.Success(configItems)
        } else {
            Log.e("parabox", "editConnectionConfig: extension not found")
            return Resource.Error("extension not found")
        }
    }

    private fun manageLifecycleOfExtensions(lifecycle: Lifecycle) {
        connectionInfoRepository.getConnectionInfoList().filter { it is Resource.Success && it.data != null }
            .map { it.data }
            .combine(connectionFlow) { pendingList, runningList ->
                Log.d("parabox", "pending=${pendingList};running=${runningList}")
                runningList.filterIsInstance<Connection.ConnectionPending>().map { connectionPending ->
                    try {
                        val job = SupervisorJob()
                        var updateExtra: JSONObject? = null
                        if (connectionPending.needReloadExtra) {
                            updateExtra = pendingList?.find { it.connectionId == connectionPending.connectionId }?.extra
                        }
                        connectionPending.toSuccess(job, updateExtra).also { connectionSuccess ->
                            val bridge = object : ParaboxBridge {
                                override suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
                                    return mainRepository.receiveMessage(msg = message, ext = connectionSuccess)
                                }

                                override suspend fun recallMessage(uuid: String): ParaboxResult {
                                    TODO("Not yet implemented")
                                }
                            }
                            lifecycle.addObserver(connectionSuccess)
                            lifecycle.coroutineScope.launch(context = CoroutineName("${connectionSuccess.name}:${connectionSuccess.alias}:${connectionSuccess.connectionId}") + CoroutineExceptionHandler { context, th ->
                                Log.e("parabox", "connection ${connectionSuccess} error", th)
                                connectionSuccess.updateStatus(ParaboxConnectionStatus.Error(th.message ?: "unknown error"))
                            } + job) {
                                if (BuildConfig.DEBUG) {
                                    launch {
                                        while (true) {
                                            delay(5000)
                                            Log.d(
                                                "parabox",
                                                " ${coroutineContext[CoroutineName.Key]} is executing on thread : ${Thread.currentThread().id}"
                                            )
                                        }
                                    }
                                }
                                connectionSuccess.init(context, bridge)
                            }
                        }
                    } catch (e: Exception) {
                        connectionPending.toFail()
                    }
                }.also {
                    updateConnections(it)
                }
                // add
                val appendReferenceIds = runningList.map { it.connectionId }.toSet()
                pendingList?.filterNot { it.connectionId in appendReferenceIds }?.forEach {
                    createAndTryAppendConnection(it)
                }
                val removeReferenceIds = pendingList?.map { it.connectionId }?.toSet() ?: emptySet()
                runningList.filterNot { it.connectionId in removeReferenceIds }.forEach {
                    (it as? Connection.ConnectionSuccess)?.run {
                        job.cancel(CancellationException("destroy"))
                        realConnection.onPause()
                        realConnection.onStop()
                        realConnection.onDestroy()
                    }
                    removeConnection(it.connectionId)
                }
            }.launchIn(lifecycle.coroutineScope)
    }

    // Some codes are referenced from
    // https://github.com/easybangumiorg/EasyBangumi/blob/main/app/src/main/java/com/heyanle/easybangumi4/extension/ExtensionController.kt
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
                e.printStackTrace()
            }
        }

        override fun onReceive(ct: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val packageName = getPackageNameFromIntent(intent)
                    if (packageName != null) {
                        try {
                            val newExtension = ExtensionLoader.scanAppWithPackageName(context, packageName)
                            if (newExtension.isNotEmpty()) {
                                _extensionFlow.update {
                                    it + newExtension
                                }
                            }
                        } catch (e: Exception) {
                            reloadExtension()
                            e.printStackTrace()
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = getPackageNameFromIntent(intent)
                    if (packageName != null) {
                        try {
                            val newExtension = ExtensionLoader.scanAppWithPackageName(context, packageName)
                            if (newExtension.isNotEmpty()) {
                                _extensionFlow.update {
                                    it.filter {
                                        when(it) {
                                            is Extension.Success.BuiltIn -> true
                                            is Extension.Success.External -> it.pkg != packageName
                                            else -> true
                                        }
                                    } + newExtension
                                }
                            }
                            GlobalScope.launch(Dispatchers.IO) {
                                connectionInfoRepository.deleteConnectionInfoByPackageName(packageName)
                            }
                        } catch (e: Exception) {
                            reloadExtension()
                            e.printStackTrace()
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    val packageName = getPackageNameFromIntent(intent)
                    if (packageName != null) {
                        try {
                            _extensionFlow.update {
                                it.filter {
                                    when(it) {
                                        is Extension.Success.BuiltIn -> true
                                        is Extension.Success.External -> it.pkg != packageName
                                        else -> true
                                    }
                                }
                            }
                            GlobalScope.launch(Dispatchers.IO) {
                                connectionInfoRepository.deleteConnectionInfoByPackageName(packageName)
                            }
                        } catch (e: Exception) {
                            reloadExtension()
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        private fun getPackageNameFromIntent(intent: Intent?): String? {
            return intent?.data?.encodedSchemeSpecificPart
        }
    }

    companion object ExtensionManager {
        private const val TAG = "ExtensionManager"
        private const val ALIAS_KEY = "alias"
        private val aliasAction = ParaboxInitAction.TextInputAction(
            key = ALIAS_KEY,
            title = "输入别名",
            errMsg = "",
            description = "别名将用于区分同一扩展提供的不同连接。",
            label = "别名",
            onResult = { alias ->
                if (alias.isEmpty()) {
                    ParaboxInitActionResult.Error("别名不能为空")
                } else {
                    ParaboxInitActionResult.Done
                }
            }
        )
        private val packageReceiverFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
    }
}