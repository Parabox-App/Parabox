package com.ojhdtapp.parabox.domain.service.extension.transport

import android.content.ComponentName
import android.content.Context
import com.ojhdtapp.parabox.domain.service.extension.SignatureVerifier
import io.grpc.ManagedChannel
import io.grpc.binder.AndroidComponentAddress
import io.grpc.binder.BindServiceFlags
import io.grpc.binder.BinderChannelBuilder
import io.grpc.binder.SecurityPolicies
import java.util.concurrent.ConcurrentHashMap

class BinderExtensionTransport(
    private val context: Context,
) {
    private data class ChannelKey(
        val packageName: String,
        val serviceClassName: String,
        val expectedSignatureSha256: String,
    )

    private val channels = ConcurrentHashMap<ChannelKey, ManagedChannel>()

    fun acquireChannel(endpoint: ExtensionEndpoint.BinderService): ManagedChannel {
        val key = ChannelKey(
            packageName = endpoint.packageName,
            serviceClassName = endpoint.serviceClassName,
            expectedSignatureSha256 = endpoint.expectedSignatureSha256,
        )
        val existing = channels[key]
        if (existing != null && !existing.isShutdown && !existing.isTerminated) {
            return existing
        }
        return channels.compute(key) { _, prev ->
            if (prev != null && !prev.isShutdown && !prev.isTerminated) {
                prev
            } else {
                val address = AndroidComponentAddress.forComponent(
                    ComponentName(endpoint.packageName, endpoint.serviceClassName)
                )
                val sha256Bytes = SignatureVerifier.hexToBytes(endpoint.expectedSignatureSha256)
                val securityPolicy = SecurityPolicies.hasSignatureSha256Hash(
                    context.packageManager,
                    endpoint.packageName,
                    sha256Bytes,
                )
                val bindFlags = BindServiceFlags.DEFAULTS.toBuilder()
                    .setImportant(true)
                    .build()
                BinderChannelBuilder.forAddress(address, context)
                    .securityPolicy(securityPolicy)
                    .setBindServiceFlags(bindFlags)
                    .build()
            }
        }!!
    }

    fun invalidateChannel(endpoint: ExtensionEndpoint.BinderService) {
        val matching = channels.keys.filter {
            it.packageName == endpoint.packageName && it.serviceClassName == endpoint.serviceClassName
        }
        matching.forEach { key ->
            channels.remove(key)?.shutdownNow()
        }
    }

    fun invalidatePackage(packageName: String) {
        val matching = channels.keys.filter { it.packageName == packageName }
        matching.forEach { key ->
            channels.remove(key)?.shutdownNow()
        }
    }

    fun shutdownAll() {
        val all = channels.values.toList()
        channels.clear()
        all.forEach { it.shutdownNow() }
    }
}
