package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11Connection
import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11InitHandler
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

object BuiltInExtensionUtil {
    fun getAllExtension() : List<Extension.Success.BuiltIn> {
        return listOf(oneBot11)
    }

    fun getConnectionByKey(key: String) : ParaboxConnection? {
        return when(key) {
            "onebot11" -> OneBot11Connection()
            else -> null
        }
    }

    val oneBot11 = Extension.Success.BuiltIn(
        name = "OneBot 11",
        icon = null,
        des = "通用聊天机器人应用接口标准（版本11）",
        key = "onebot11",
        initHandler = OneBot11InitHandler()
    )
}