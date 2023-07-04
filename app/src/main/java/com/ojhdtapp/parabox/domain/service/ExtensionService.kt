package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExtensionService : LifecycleService() {
    @Inject lateinit var extensionManager: ExtensionManager

    private var bridge: ExtensionServiceBridge? = null

    val isConnected = MutableStateFlow<Boolean>(false)

    fun setBridge(mBridge: ExtensionServiceBridge){
        bridge = mBridge
    }

    fun attachLifecycleToExtensions(){
        lifecycleScope.launch {
            extensionManager.installedExtensionsFlow.collectLatest {
                it.forEach {
                    lifecycle.addObserver(it.ext)
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
        attachLifecycleToExtensions()
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

}

