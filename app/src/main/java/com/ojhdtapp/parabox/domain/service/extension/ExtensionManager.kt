package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class ExtensionManager(
    val context: Context,
    val extensionInfoRepository: ExtensionInfoRepository
) {
    // Pkg
    private val _extensionPkgFlow = MutableStateFlow(emptyList<PackageInfo>())
    val extensionPkgFlow = _extensionPkgFlow.asStateFlow()

    // Init
    private val _extensionFlow = MutableStateFlow(emptyList<Extension>())
    val extensionFlow = _extensionFlow.asStateFlow()

    // InitAction
    private val _initActionWrapperFlow = MutableStateFlow(ExtensionInitActionWrapper())
    val initActionWrapperFlow = _initActionWrapperFlow.asStateFlow()
    private var initHandler: ParaboxInitHandler? = null
    private var getInitActionJob: Job? = null
    private var awaitInitActionResJob: Job? = null

    suspend fun initNewExtensionConnection(packageInfo: PackageInfo) {
        if (initHandler == null || initActionWrapperFlow.value.packageInfo?.packageName != packageInfo.packageName) {
            initHandler = ExtensionLoader.createInitHandler(context, packageInfo)
            _initActionWrapperFlow.value =
                ExtensionInitActionWrapper(
                    packageInfo = packageInfo,
                    actionList = withContext(Dispatchers.IO) {
                        initHandler?.getExtensionInitActions(packageInfo, emptyList(), 0) ?: emptyList()
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
        if (initActionWrapperFlow.value.packageInfo == null || initHandler == null) {
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

    fun resetInitAction(isDone: Boolean) {
        if (isDone && initHandler != null && initActionWrapperFlow.value.packageInfo != null) {
            addPendingExtension(
                alias = "#TODO: alias",
                packageInfo = initActionWrapperFlow.value.packageInfo!!,
                extra = initHandler!!.data
            )
        }

        _initActionWrapperFlow.value = ExtensionInitActionWrapper()
        initHandler = null
        getInitActionJob?.cancel()
        getInitActionJob = null
        awaitInitActionResJob?.cancel()
        awaitInitActionResJob = null
    }

    private suspend fun increaseInitActionStep() {
        _initActionWrapperFlow.value = initActionWrapperFlow.value.copy(
            actionList = withContext(Dispatchers.IO) {
                initHandler?.getExtensionInitActions(
                    initActionWrapperFlow.value.packageInfo!!,
                    initActionWrapperFlow.value.actionList,
                    initActionWrapperFlow.value.currentIndex + 1
                ) ?: emptyList()
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
                alias, packageInfo.packageName, extName, versionName, versionCode, extra
            )
        )
    }

    fun removePendingExtension(extensionId: Long): Boolean {
        return extensionInfoRepository.deleteExtensionInfoById(extensionId) > -1
    }

    fun createAndTryAppendExtension(extensionInfo: ExtensionInfo) {
        val extension = ExtensionLoader.createExtension(context, extensionInfo)
        _extensionFlow.update {
            it + extension
        }
        Log.d("parabox", "append extension=${_extensionFlow.value}")
    }

    // replace the old extension with the new one if the extensionId is the same
    fun updateExtensions(newExtensionList: List<Extension>) {
        _extensionFlow.update {
            it.toMutableList().apply {
                replaceAll {
                    newExtensionList.find { newExtension -> it.extensionId == newExtension.extensionId } ?: it
                }
            }
        }
    }

    fun removeExtension(extensionId: Long) {
        _extensionFlow.update {
            it.filter { it.extensionId != extensionId }
        }
    }

    fun refreshExtensionPkg() {
        _extensionPkgFlow.value = ExtensionLoader.getExtensionPkgInfo(context)
    }

    // Only ExtensionSuccess, no Context and bridge changed
    fun restartExtension(extensionId: Long) {
        _extensionFlow.value.find { it.extensionId == extensionId }?.also {
            if (it is Extension.ExtensionSuccess) {
                try {
                    it.ext.updateStatus(ParaboxExtensionStatus.Pending)
                    it.job.cancel(CancellationException("restart"))
                    it.ext.onPause()
                    it.ext.onStop()
                    it.ext.onDestroy()
                    updateExtensions(listOf(Extension.ExtensionPending(it)))
                } catch (e: Exception) {
                    it.ext.updateStatus(ParaboxExtensionStatus.Error(e.message ?: "restart error"))
                    Log.e("parabox", "restartExtension error", e)
                }
            }
        }
    }
}