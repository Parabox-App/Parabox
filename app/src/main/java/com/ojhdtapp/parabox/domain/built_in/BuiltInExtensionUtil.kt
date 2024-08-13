package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11
import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection

object BuiltInExtensionUtil {
    fun getAllExtension() : List<Extension.Success.BuiltIn> {
        return listOf(OneBot11.extension)
    }

    fun getConnectionByKey(key: String) : ParaboxConnection? {
        return when(key) {
            "onebot11" -> OneBot11Connection()
            else -> null
        }
    }

}