package com.ojhdtapp.parabox.domain.built_in

import com.ojhdtapp.parabox.domain.built_in.onebot11.OneBot11
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionGrpcEngine
import io.grpc.Server
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.runBlocking

object BuiltInExtensionUtil {
    const val IN_PROCESS_SERVER_NAME = "parabox-builtin-extensions-v2"

    private val lock = Any()
    private var server: Server? = null
    private var engine: ParaboxExtensionGrpcEngine? = null

    fun ensureStarted() {
        synchronized(lock) {
            if (server != null && server?.isShutdown == false) return
            val grpcEngine = ParaboxExtensionGrpcEngine(
                providers = listOf(OneBot11.provider),
                versionName = "2.0.0",
                versionCode = 2L,
            )
            val builtServer = InProcessServerBuilder
                .forName(IN_PROCESS_SERVER_NAME)
                .addService(grpcEngine)
                .build()
                .start()
            engine = grpcEngine
            server = builtServer
        }
    }

    fun shutdown() {
        synchronized(lock) {
            val currentEngine = engine
            val currentServer = server
            engine = null
            server = null
            if (currentEngine != null) {
                runBlocking { currentEngine.shutdownAllSessions() }
            }
            currentServer?.shutdownNow()
        }
    }

    fun getAllExtension(): List<Extension.Success.BuiltIn> = listOf(OneBot11.extension)

    fun getEndpointByKey(key: String): ExtensionEndpoint.InProcess? {
        ensureStarted()
        return getAllExtension().find { it.key == key }?.endpoint
    }
}
