package com.ojhdtapp.parabox.domain.model

import androidx.compose.ui.graphics.ImageBitmap
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

sealed interface Extension {
    val name: String
    val icon: ImageBitmap?
    val des: String?
    val key: String
    sealed interface Success : Extension {
        val initHandler: ParaboxInitHandler
        val connectionClassName: String
        data class BuiltIn(
            override val name: String,
            override val icon: ImageBitmap?,
            override val des: String?,
            override val key: String,
            override val initHandler: ParaboxInitHandler,
            override val connectionClassName: String
        ) : Success, BuiltInExtension
        data class External(
            override val name: String,
            override val icon: ImageBitmap?,
            override val des: String?,
            override val key: String,
            override val initHandler: ParaboxInitHandler,
            override val connectionClassName: String,
            override val version: String,
            override val versionCode: Long,
            override val pkg: String
        ) : Success, ExternalExtension
    }
    data class Error(
        override val name: String,
        override val icon: ImageBitmap?,
        override val des: String?,
        override val key: String,
        val errMsg: String
    ): Extension
}

interface BuiltInExtension {

}

interface ExternalExtension {
    val version: String
    val versionCode: Long
    val pkg: String
}
