package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

interface BuiltInConnection {
    val key: String
    val connection: Connection.BuiltInConnection
    val initHandler: ParaboxInitHandler
    val extension: ParaboxExtension
}