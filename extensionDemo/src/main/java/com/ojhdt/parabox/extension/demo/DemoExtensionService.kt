package com.ojhdt.parabox.extension.demo

import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionService

class DemoExtensionService : ParaboxExtensionService() {
    override fun provideExtensions(): List<ParaboxExtensionProvider> {
        return listOf(DemoExtensionProvider(applicationContext))
    }
}
