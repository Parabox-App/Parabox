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
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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
        extensionInfoRepository.getExtensionInfoList().filter { it is Resource.Success && it.data != null }.map { it.data }
            .combine(extensionManager.extensionFlow) { pendingList, runningList ->
                Log.d("bbb", "pending=${pendingList};running=${runningList}")
                runningList.filterIsInstance<Extension.ExtensionPending>().map { Extension.ExtensionSuccess(it) }.forEach {
                    Log.d("bbb", "real init for ${it}")
                    val bridge = object : ParaboxBridge {
                        override suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
                            return mainRepository.receiveMessage(msg = message, ext = it)
                        }

                        override suspend fun recallMessage(uuid: String): ParaboxResult {
                            TODO("Not yet implemented")
                        }

                    }
                    lifecycle.addObserver(it)
                    it.init(baseContext, bridge)
                    extensionManager.updateExtension(it)
                }
                // add
                val appendReferenceIds = runningList.map { it.extensionId }.toSet()
                pendingList?.filterNot { it.extensionId in appendReferenceIds }?.forEach {
                    Log.d("bbb", "add pending extension=$it")
                    extensionManager.createAndTryAppendExtension(it)
                }
                val removeReferenceIds = pendingList?.map { it.extensionId }?.toSet() ?: emptySet()
                runningList.filterNot { it.extensionId in removeReferenceIds }.forEach {
                    Log.d("bbb", "remove deleted extension=$it")
                    (it as? Extension.ExtensionSuccess)?.onDestroy(this@ExtensionService)
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


//        lifecycleScope.launch(Dispatchers.IO) {
//            delay(5000)
//            extensionManager.extensionPkgFlow.value.firstOrNull()?.let{
//                Log.d("bbb", "add pkgInfo")
//                extensionManager.addPendingExtension("hahaha", it, "")
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

