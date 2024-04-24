package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newCoroutineContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

@AndroidEntryPoint
class ExtensionService : LifecycleService() {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @Inject
    lateinit var extensionInfoRepository: ExtensionInfoRepository

    private var bridge: ExtensionServiceBridge? = null

    val isConnected = MutableStateFlow<Boolean>(false)

    fun setBridge(mBridge: ExtensionServiceBridge) {
        bridge = mBridge
    }


    private fun manageLifecycleOfExtensions() {
        extensionInfoRepository.getExtensionInfoList().filter { it is Resource.Success && it.data != null }
            .map { it.data }
            .combine(extensionManager.extensionFlow) { pendingList, runningList ->
                Log.d("parabox", "pending=${pendingList};running=${runningList}")
                runningList.filterIsInstance<Extension.ExtensionPending>().map {
                    try {
                        val job = SupervisorJob()
                        Extension.ExtensionSuccess(it, job).also {
                            val bridge = object : ParaboxBridge {
                                override suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
                                    return mainRepository.receiveMessage(msg = message, ext = it)
                                }

                                override suspend fun recallMessage(uuid: String): ParaboxResult {
                                    TODO("Not yet implemented")
                                }
                            }
                            lifecycle.addObserver(it)
                            lifecycleScope.launch(context = CoroutineName("${it.pkg}:${it.alias}:${it.extensionId}") + CoroutineExceptionHandler { context, th ->
                                Log.e("parabox", "extension ${it} error", th)
                                it.updateStatus(ParaboxExtensionStatus.Error(th.message ?: "unknown error"))
                            } + job) {
                                it.init(baseContext, bridge, it.extra)
                            }
                        }
                    } catch (e: Exception) {
                        Extension.ExtensionFail(it)
                    }
                }.also {
                    extensionManager.updateExtensions(it)
                }
                // add
                val appendReferenceIds = runningList.map { it.extensionId }.toSet()
                pendingList?.filterNot { it.extensionId in appendReferenceIds }?.forEach {
                    extensionManager.createAndTryAppendExtension(it)
                }
                val removeReferenceIds = pendingList?.map { it.extensionId }?.toSet() ?: emptySet()
                runningList.filterNot { it.extensionId in removeReferenceIds }.forEach {
                    (it as? Extension.ExtensionSuccess)?.run {
                        job.cancel(CancellationException("destroy"))
                        ext.onPause()
                        ext.onStop()
                        ext.onDestroy()
                    }
                    extensionManager.removeExtension(it.extensionId)
                }
            }.launchIn(lifecycleScope)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return ExtensionServiceBinder()
    }

    interface ExtensionServiceBridge

    inner class ExtensionServiceBinder : Binder() {
        fun getService(): ExtensionService {
            return this@ExtensionService
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("parabox", "extension service connected")
        notificationUtil.startForegroundService(this)
        manageLifecycleOfExtensions()
        extensionManager.refreshExtensionPkg()

        lifecycleScope.launch {
            extensionManager.extensionFlow.collectLatest {
                Log.d("parabox", "extensionFlow=${it}")
            }
        }
//
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            delay(5000)
//            extensionManager.extensionPkgFlow.value.firstOrNull()?.let {
//                if (extensionManager.extensionFlow.value.isEmpty()) {
//                    Log.d("bbb", "add pkgInfo")
//                    extensionManager.addPendingExtension("test", it, Bundle())
//                }
//            }
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("parabox", "extension service started")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("parabox", "extension service death")
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

}

