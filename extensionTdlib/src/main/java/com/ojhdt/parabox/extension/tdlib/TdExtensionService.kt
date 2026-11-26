package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdNetworkMonitor
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TdExtensionService : ParaboxExtensionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var networkMonitor: TdNetworkMonitor? = null

    override fun onCreate() {
        super.onCreate()
        val coordinator = TdExtensionContainer.getCoordinator(applicationContext)
        networkMonitor = TdNetworkMonitor(applicationContext).also { monitor ->
            monitor.start { networkType ->
                serviceScope.launch {
                    coordinator.broadcastNetworkType(networkType)
                }
            }
        }
    }

    override fun provideExtensions(): List<ParaboxExtensionProvider> {
        val coordinator = TdExtensionContainer.getCoordinator(applicationContext)
        val mediaBridge = TdExtensionContainer.getMediaBridge(applicationContext)
        return listOf(TdExtensionProvider(coordinator, mediaBridge))
    }

    override fun onDestroy() {
        networkMonitor?.stop()
        networkMonitor = null
        serviceScope.cancel()
        runBlocking {
            TdExtensionContainer.getCoordinator(applicationContext).shutdownAll()
        }
        super.onDestroy()
    }
}
