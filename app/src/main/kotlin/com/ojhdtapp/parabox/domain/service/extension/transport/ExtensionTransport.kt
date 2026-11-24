package com.ojhdtapp.parabox.domain.service.extension.transport

import io.grpc.ManagedChannel

interface ExtensionTransport {
    fun acquireChannel(endpoint: ExtensionEndpoint): ManagedChannel
    fun invalidateChannel(endpoint: ExtensionEndpoint)
    fun invalidatePackage(packageName: String)
    fun shutdownAll()
}

class CompositeExtensionTransport(
    private val inProcessTransport: InProcessExtensionTransport,
    private val binderTransport: BinderExtensionTransport,
) : ExtensionTransport {

    override fun acquireChannel(endpoint: ExtensionEndpoint): ManagedChannel = when (endpoint) {
        is ExtensionEndpoint.InProcess -> inProcessTransport.acquireChannel(endpoint)
        is ExtensionEndpoint.BinderService -> binderTransport.acquireChannel(endpoint)
    }

    override fun invalidateChannel(endpoint: ExtensionEndpoint) {
        when (endpoint) {
            is ExtensionEndpoint.InProcess -> inProcessTransport.invalidateChannel(endpoint)
            is ExtensionEndpoint.BinderService -> binderTransport.invalidateChannel(endpoint)
        }
    }

    override fun invalidatePackage(packageName: String) {
        binderTransport.invalidatePackage(packageName)
    }

    override fun shutdownAll() {
        inProcessTransport.shutdownAll()
        binderTransport.shutdownAll()
    }
}
