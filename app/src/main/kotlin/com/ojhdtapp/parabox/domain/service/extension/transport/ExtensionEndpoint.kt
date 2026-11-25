package com.ojhdtapp.parabox.domain.service.extension.transport

sealed interface ExtensionEndpoint {
    val extensionKey: String

    data class InProcess(
        val serverName: String,
        override val extensionKey: String,
    ) : ExtensionEndpoint

    data class BinderService(
        val packageName: String,
        val serviceClassName: String,
        override val extensionKey: String,
        val expectedSignatureSha256: String,
    ) : ExtensionEndpoint
}
