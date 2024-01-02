package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExtensionService : LifecycleService() {
    @Inject lateinit var extensionManager: ExtensionManager
    @Inject lateinit var mainRepository: MainRepository
    @Inject lateinit var notificationUtil: NotificationUtil

    private var bridge: ExtensionServiceBridge? = null

    val isConnected = MutableStateFlow<Boolean>(false)

    fun setBridge(mBridge: ExtensionServiceBridge){
        bridge = mBridge
    }

    fun attachLifecycleToExtensions(){
        lifecycleScope.launch {
            extensionManager.installedExtensionsFlow.collectLatest {
                it.forEach {
                    val bridge = object: ParaboxBridge {
                        override suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
                            return mainRepository.receiveMessage(msg = message, ext = it)
                        }

                        override suspend fun recallMessage(uuid: String): ParaboxResult {
                            TODO("Not yet implemented")
                        }

                    }
                    lifecycle.addObserver(it.ext)
                    it.ext.init(baseContext, bridge)
                }
            }
        }
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        notificationUtil.startForegroundService(this)
        attachLifecycleToExtensions()
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

}

