package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

interface BuiltInExtension {
    val key: String
    val extensionInfo: ExtensionInfo.BuiltInExtensionInfo
    val initHandler: ParaboxInitHandler
    val connection: ParaboxConnection
}