package com.ojhdt.parabox.extension.demo.extension_a

import com.ojhdt.parabox.extension.demo.R
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

class ExtensionA(
) : ParaboxExtension {
    override fun getKey(): String {
        return "extension_a"
    }

    override fun getName(): String? {
        return "Test Onebot Extension"
    }

    override fun getDescription(): String? {
        return "Test Onebot Extension"
    }

    override fun getIconResId(): Int? {
        return R.mipmap.icon
    }

    override fun getInitHandler(): ParaboxInitHandler {
        return InitHandler()
    }

    override fun getConnectionClassName(): String {
        return Connection::class.java.name
    }
}