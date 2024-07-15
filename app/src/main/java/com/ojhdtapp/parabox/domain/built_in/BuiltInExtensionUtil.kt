package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

object BuiltInExtensionUtil {
    fun getConnectionCardModelList() : List<ExtensionInfo.BuiltInExtensionInfo> {
        return listOf(OneBot11.extensionInfo)
    }

    fun getInitHandlerByKey(key: String): ParaboxInitHandler? {
        return when(key) {
            OneBot11.key -> OneBot11.initHandler
            else -> null
        }
    }

    fun getExtensionByKey(key: String) : ParaboxConnection? {
        return when(key) {
            OneBot11.key -> OneBot11.connection
            else -> null
        }
    }
}