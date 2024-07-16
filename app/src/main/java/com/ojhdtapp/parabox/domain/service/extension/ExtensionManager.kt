package com.ojhdtapp.parabox.domain.service.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.ExtensionInfoType
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import com.ojhdtapp.parabox.domain.built_in.BuiltInExtensionUtil
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.BuildConfig
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class ExtensionManager(
    val context: Context,
    val mainRepository: MainRepository,
    val extensionInfoRepository: ExtensionInfoRepository
) : DefaultLifecycleObserver {
    // Extension
    private val _extensionFlow = MutableStateFlow(emptyList<Extension>())
    val extensionFlow get() = _extensionFlow.asStateFlow().map { it. }

    // Pkg
    private val _extensionPkgFlow = MutableStateFlow(emptyList<PackageInfo>())
    val extensionPkgFlow = _extensionPkgFlow.asStateFlow()

    // Init
    private val _connectionFlow = MutableStateFlow(emptyList<Connection>())
    val connectionFlow = _connectionFlow.asStateFlow()

    // InitAction
    private val _initActionWrapperFlow = MutableStateFlow(ExtensionInitActionWrapper())
    val initActionWrapperFlow = _initActionWrapperFlow.asStateFlow()
    private var initHandler: ParaboxInitHandler? = null
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

    suspend fun initNewExtensionConnection(extensionInfo: com.ojhdtapp.parabox.domain.model.ExtensionInfo) {
        if (initHandler == null || initActionWrapperFlow.value.key != extensionInfo.getKey()) {
            initHandler = when (extensionInfo) {
                is com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.BuiltInExtensionInfo -> BuiltInExtensionUtil.getInitHandlerByKey(
                    extensionInfo.builtInKey
                )

                is com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.ExtendExtensionInfo -> ExtensionLoader.createInitHandler(
                    context,
                    extensionInfo.packageInfo
                )
            }
            val initActions = try {
                initHandler?.getExtensionInitActions(emptyList(), 0)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            _initActionWrapperFlow.value =
                ExtensionInitActionWrapper(
                    key = extensionInfo.getKey(),
                    name = extensionInfo.name,
                    extensionInfo = extensionInfo,
                    actionList = withContext(Dispatchers.IO) {
                        listOf(ExtensionManager.aliasAction) + (initActions ?: emptyList())
                    },
                    currentIndex = 0
                )
        } else {
            Log.d("parabox", "initNewExtensionConnection: already init")
        }
    }

    suspend fun revertInitAction() {
        awaitInitActionResJob?.cancel()
        _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
            currentIndex = initActionWrapperFlow.value.currentIndex - 1
        )
    }

    suspend fun submitInitActionResult(result: Any) {
        if (initActionWrapperFlow.value.key == null || initHandler == null) {
            Log.e("parabox", "submitInitActionResult: packageInfo or initHandler is null")
            return
        }
        awaitInitActionResJob?.cancel()
        coroutineScope {
            val currentActionIndex = initActionWrapperFlow.value.currentIndex
            val action = initActionWrapperFlow.value.actionList.getOrNull(currentActionIndex) ?: return@coroutineScope
            awaitInitActionResJob = launch(Dispatchers.IO) {
                when (action) {
                    is ParaboxInitAction.InfoAction -> {
                        val res = action.onResult()
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initHandler?.data?.putString(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
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
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initHandler?.data?.putString(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
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
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initHandler?.data?.putString(action.key, action.options[result])
                            increaseInitActionStep()
                        } else {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
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
                        val res = action.onResult(result.toString())
                        if (res is ParaboxInitActionResult.Done) {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
                                    set(
                                        currentActionIndex, action.copy(
                                            errMsg = ""
                                        )
                                    )
                                }
                            )
                            initHandler?.data?.putString(action.key, result.toString())
                            increaseInitActionStep()
                        } else {
                            _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
                                actionList = initActionWrapperFlow.value.actionList.toMutableList().apply {
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
        if (isDone && initHandler != null && initActionWrapperFlow.value.key != null) {
            coroutineScope {
                launch(Dispatchers.IO) {
                    when (initActionWrapperFlow.value.extensionInfo) {
                        is com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.BuiltInExtensionInfo -> {
                            addBuiltInPendingExtension(
                                alias = initHandler!!.data.getString(
                                    ALIAS_KEY, initActionWrapperFlow.value.key ?: "alias"
                                ),
                                name = (initActionWrapperFlow.value.extensionInfo as com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.BuiltInExtensionInfo).name,
                                key = (initActionWrapperFlow.value.extensionInfo as com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.BuiltInExtensionInfo).builtInKey,
                                extra = initHandler!!.data
                            )
                        }

                        is com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.ExtendExtensionInfo -> {
                            addPendingExtension(
                                alias = initHandler!!.data.getString(
                                    ALIAS_KEY, initActionWrapperFlow.value.key ?: "alias"
                                ),
                                packageInfo = (initActionWrapperFlow.value.extensionInfo as com.ojhdtapp.parabox.domain.model.Connection.ExtensionInfo.ExtendExtensionInfo).packageInfo,
                                extra = initHandler!!.data
                            )
                        }

                        else -> {

                        }
                    }
                }
            }

        }

        _initActionWrapperFlow.value = ExtensionInitActionWrapper()
        initHandler = null
        getInitActionJob?.cancel()
        getInitActionJob = null
        awaitInitActionResJob?.cancel()
        awaitInitActionResJob = null
    }

    private suspend fun increaseInitActionStep() {
        val initActions = try {
            initHandler?.getExtensionInitActions(
                initActionWrapperFlow.value.actionList,
                initActionWrapperFlow.value.currentIndex + 1
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
            actionList = withContext(Dispatchers.IO) {
                listOf(ExtensionManager.aliasAction) + (initActions ?: emptyList())
            },
            currentIndex = initActionWrapperFlow.value.currentIndex + 1
        )
    }

    fun addPendingExtension(alias: String, packageInfo: PackageInfo, extra: Bundle): Long {
        val pkgManager = context.packageManager
        val appInfo = try {
            pkgManager.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return -1L
        }
        val extName = pkgManager.getApplicationLabel(appInfo).toString()
        val versionName = packageInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        if (versionName.isNullOrEmpty()) {
            return -1L
        }
        return extensionInfoRepository.insertExtensionInfo(
            ExtensionInfoEntity(
                alias,
                extName,
                ExtensionInfoType.Extend.ordinal,
                extra,
                packageInfo.packageName,
                versionName,
                versionCode
            )
        )
    }

    fun addBuiltInPendingExtension(alias: String, key: String, name: String, extra: Bundle): Long {
        return extensionInfoRepository.insertExtensionInfo(
            ExtensionInfoEntity(
                alias, name, ExtensionInfoType.BuiltIn.ordinal, extra, "", "", 0, key
            )
        )
    }

    fun removePendingExtension(extensionId: Long): Boolean {
        return extensionInfoRepository.deleteExtensionInfoById(extensionId) > -1
    }

    fun createAndTryAppendExtension(extensionInfo: ExtensionInfo) {
        val extension = when (extensionInfo.type) {
            ExtensionInfoType.BuiltIn -> {
                createBuiltInExtension(extensionInfo)
            }

            ExtensionInfoType.Extend -> {
                ExtensionLoader.createExtension(context, extensionInfo)
            }
        }
        _connectionFlow.update {
            it + extension
        }
        Log.d("parabox", "append extension=${_connectionFlow.value}")
    }

    private fun createBuiltInExtension(extensionInfo: ExtensionInfo): Connection {
        return try {
            val ext = BuiltInExtensionUtil.getExtensionByKey(extensionInfo.builtInKey)
            if (ext != null) {
                Connection.ConnectionPending.BuiltInConnectionPending(
                    extensionInfo, ext
                )
            } else {
                Connection.ConnectionFail.BuiltInConnectionFail(extensionInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Connection.ConnectionFail.BuiltInConnectionFail(extensionInfo)
        }
    }

    // replace the old extension with the new one if the extensionId is the same
    fun updateExtensions(newConnectionList: List<Connection>) {
        _connectionFlow.update {
            it.toMutableList().apply {
                replaceAll {
                    newConnectionList.find { newExtension -> it.extensionId == newExtension.extensionId } ?: it
                }
            }
        }
    }

    fun removeExtension(extensionId: Long) {
        _connectionFlow.update {
            it.filter { it.extensionId != extensionId }
        }
    }

    fun refreshExtensionPkg() {
        _extensionPkgFlow.value = ExtensionLoader.getExtensionPkgInfo(context)
    }

    // Only ExtensionSuccess, no Context and bridge changed
    fun restartExtension(extensionId: Long) {
        _connectionFlow.value.find { it.extensionId == extensionId }?.also {
            if (it is Connection.ConnectionSuccess) {
                try {
                    it.ext.updateStatus(ParaboxConnectionStatus.Pending)
                    it.job.cancel(CancellationException("restart"))
                    it.ext.onPause()
                    it.ext.onStop()
                    it.ext.onDestroy()
                    updateExtensions(listOf(it.toPending()))
                } catch (e: Exception) {
                    it.ext.updateStatus(ParaboxConnectionStatus.Error(e.message ?: "restart error"))
                    Log.e("parabox", "restartExtension error", e)
                }
            }
        }
    }

    private fun manageLifecycleOfExtensions(lifecycle: Lifecycle) {
        extensionInfoRepository.getExtensionInfoList().filter { it is Resource.Success && it.data != null }
            .map { it.data }
            .combine(connectionFlow) { pendingList, runningList ->
                Log.d("parabox", "pending=${pendingList};running=${runningList}")
                runningList.filterIsInstance<Connection.ConnectionPending>().map {
                    try {
                        val job = SupervisorJob()
                        it.toSuccess(job).also {
                            val bridge = object : ParaboxBridge {
                                override suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
                                    return mainRepository.receiveMessage(msg = message, ext = it)
                                }

                                override suspend fun recallMessage(uuid: String): ParaboxResult {
                                    TODO("Not yet implemented")
                                }
                            }
                            lifecycle.addObserver(it)
                            lifecycle.coroutineScope.launch(context = CoroutineName("${it.name}:${it.alias}:${it.extensionId}") + CoroutineExceptionHandler { context, th ->
                                Log.e("parabox", "extension ${it} error", th)
                                it.updateStatus(ParaboxConnectionStatus.Error(th.message ?: "unknown error"))
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
                                it.init(context, bridge, it.extra)
                            }
                        }
                    } catch (e: Exception) {
                        it.toFail()
                    }
                }.also {
                    updateExtensions(it)
                }
                // add
                val appendReferenceIds = runningList.map { it.extensionId }.toSet()
                pendingList?.filterNot { it.extensionId in appendReferenceIds }?.forEach {
                    createAndTryAppendExtension(it)
                }
                val removeReferenceIds = pendingList?.map { it.extensionId }?.toSet() ?: emptySet()
                runningList.filterNot { it.extensionId in removeReferenceIds }.forEach {
                    (it as? Connection.ConnectionSuccess)?.run {
                        job.cancel(CancellationException("destroy"))
                        realConnection.onPause()
                        realConnection.onStop()
                        realConnection.onDestroy()
                    }
                    removeExtension(it.extensionId)
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
                            val pkgInfo = context.packageManager.getPackageInfo(packageName, 0)
                            if (ExtensionLoader.isPackageAnExtension(pkgInfo)) {

                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = getPackageNameFromIntent(intent)
                    if (packageName != null) {
                        ...
                    }
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    val packageName = getPackageNameFromIntent(intent)
                    if (packageName != null) {
                        ...
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