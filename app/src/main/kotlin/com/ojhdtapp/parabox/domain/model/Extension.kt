package com.ojhdtapp.parabox.domain.model

import androidx.compose.ui.graphics.ImageBitmap
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint

sealed interface Extension {
    val name: String
    val icon: ImageBitmap?
    val des: String?
    val key: String

    sealed interface Success : Extension {
        val endpoint: ExtensionEndpoint

        data class BuiltIn(
            override val name: String,
            override val icon: ImageBitmap?,
            override val des: String?,
            override val key: String,
            override val endpoint: ExtensionEndpoint.InProcess,
        ) : Success, BuiltInExtension

        data class External(
            override val name: String,
            override val icon: ImageBitmap?,
            override val des: String?,
            override val key: String,
            override val endpoint: ExtensionEndpoint.BinderService,
            override val serviceClassName: String,
            override val version: String,
            override val versionCode: Long,
            override val pkg: String,
            val signatureSha256: String,
        ) : Success, ExternalExtension
    }

    data class Untrusted(
        override val name: String,
        override val icon: ImageBitmap?,
        override val des: String?,
        override val key: String,
        val pkg: String,
        val serviceClassName: String,
        val version: String,
        val versionCode: Long,
        val signatureSha256: String,
        val signatureMismatch: Boolean = false,
    ) : Extension

    data class Error(
        override val name: String,
        override val icon: ImageBitmap?,
        override val des: String?,
        override val key: String,
        val errMsg: String,
    ) : Extension
}

interface BuiltInExtension

interface ExternalExtension {
    val serviceClassName: String
    val version: String
    val versionCode: Long
    val pkg: String
}
