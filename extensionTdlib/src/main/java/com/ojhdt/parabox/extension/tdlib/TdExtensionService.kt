package com.ojhdt.parabox.extension.tdlib

import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionService
import kotlinx.coroutines.runBlocking

class TdExtensionService : ParaboxExtensionService() {
    override fun provideExtensions(): List<ParaboxExtensionProvider> {
        val coordinator = TdExtensionContainer.getCoordinator(applicationContext)
        val mediaBridge = TdExtensionContainer.getMediaBridge(applicationContext)
        return listOf(TdExtensionProvider(coordinator, mediaBridge))
    }

    override fun onDestroy() {
        runBlocking {
            TdExtensionContainer.getCoordinator(applicationContext).shutdownAll()
        }
        super.onDestroy()
    }
}
