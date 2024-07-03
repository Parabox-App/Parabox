package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

object BuiltInConnectionUtil {
    fun getConnectionCardModelList() : List<Connection.BuiltInConnection> {
        return listOf(OneBot11.connection)
    }

    fun getInitHandlerByKey(key: String): ParaboxInitHandler? {
        return when(key) {
            OneBot11.key -> OneBot11.initHandler
            else -> null
        }
    }

    fun getExtensionByKey(key: String) : ParaboxExtension? {
        return when(key) {
            OneBot11.key -> OneBot11.extension
            else -> null
        }
    }
}