package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.paraboxdevelopmentkit.BuildConfig
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        lifecycle.addObserver(extensionManager)

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
        lifecycle.removeObserver(notificationUtil)
        super.onDestroy()
    }

}

