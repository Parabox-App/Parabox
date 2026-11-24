package com.ojhdtapp.parabox.domain.service.extension.transport

import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import java.util.concurrent.ConcurrentHashMap

class InProcessExtensionTransport {
    private val channels = ConcurrentHashMap<String, ManagedChannel>()

    fun acquireChannel(endpoint: ExtensionEndpoint.InProcess): ManagedChannel {
        val existing = channels[endpoint.serverName]
        if (existing != null && !existing.isShutdown && !existing.isTerminated) {
            return existing
        }
        return channels.compute(endpoint.serverName) { _, prev ->
            if (prev != null && !prev.isShutdown && !prev.isTerminated) {
                prev
            } else {
                InProcessChannelBuilder.forName(endpoint.serverName)
                    .build()
            }
        }!!
    }

    fun invalidateChannel(endpoint: ExtensionEndpoint.InProcess) {
        channels.remove(endpoint.serverName)?.shutdownNow()
    }

    fun shutdownAll() {
        val all = channels.values.toList()
        channels.clear()
        all.forEach { it.shutdownNow() }
    }
}
