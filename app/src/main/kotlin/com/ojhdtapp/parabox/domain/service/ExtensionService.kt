package com.ojhdtapp.parabox.domain.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.repository.ConnectionInfoRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionConnectionSnapshot
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.domain.service.extension.ExtensionMonitorTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    lateinit var connectionInfoRepository: ConnectionInfoRepository

    private var bridge: ExtensionServiceBridge? = null
    private val monitorTracker = ExtensionMonitorTracker()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("parabox", "extension service connected")
        notificationUtil.startForegroundService(this)
        lifecycle.addObserver(extensionManager)

        lifecycleScope.launch {
            extensionManager.connectionFlow
                .flatMapLatest { connections ->
                    if (connections.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val snapshotFlows = connections.map { connection ->
                            when (connection) {
                                is Connection.ConnectionSuccess -> connection.getStatus().map { status ->
                                    ExtensionConnectionSnapshot.fromSuccess(connection, status)
                                }

                                is Connection.ConnectionPending -> flowOf(
                                    ExtensionConnectionSnapshot.fromPending(connection)
                                )

                                is Connection.ConnectionFail -> flowOf(
                                    ExtensionConnectionSnapshot.fromFail(connection)
                                )
                            }
                        }
                        combine(snapshotFlows) { it.toList() }
                    }
                }
                .distinctUntilChanged()
                .collect { snapshots ->
                    val update = monitorTracker.onSnapshotsUpdated(snapshots)
                    isConnected.value = update.summary.activeCount > 0
                    notificationUtil.updateForegroundServiceNotification(update.summary)
                    update.clearedAlertConnectionIds.forEach { connectionId ->
                        notificationUtil.cancelExtensionDisconnectNotification(connectionId)
                    }
                    update.disconnectAlerts.forEach { alert ->
                        notificationUtil.sendExtensionDisconnectNotification(alert)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("parabox", "extension service started")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("parabox", "extension service death")
        stopForeground(STOP_FOREGROUND_DETACH)
        lifecycle.removeObserver(extensionManager)
        super.onDestroy()
    }

}

