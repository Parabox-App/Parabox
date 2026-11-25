package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionTransport
import com.ojhdtapp.parabox.domain.service.extension.transport.InProcessExtensionTransport
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionDescriptor
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionGrpcEngine
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxInitStepResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ExtensionSessionClientTest {

    private class ReconnectTestProvider(
        private val initCount: AtomicInteger,
    ) : ParaboxExtensionProvider {
        override val descriptor = ParaboxExtensionDescriptor(
            key = "reconnect_test",
            name = "Reconnect Test",
        )

        override suspend fun onInitStep(
            wizardSessionId: String,
            currentStepIndex: Int,
            submittedKey: String?,
            submittedValue: Any?,
            accumulatedExtra: JSONObject,
        ): ParaboxInitStepResult = ParaboxInitStepResult(emptyList(), 0, completed = true, updatedExtra = accumulatedExtra)

        override fun createSession(
            sessionId: String,
            extra: JSONObject,
            hostPackageName: String,
        ): ParaboxSession = object : ParaboxSession(sessionId, extra, hostPackageName) {
            override suspend fun onInitialize(): Boolean {
                initCount.incrementAndGet()
                updateStatus(ParaboxConnectionStatus.Active)
                return true
            }

            override suspend fun onSendMessage(message: SendMessage): ParaboxResult {
                emitMessage(
                    ReceiveMessage(
                        contents = listOf(ParaboxPlainText("ack-from-server")),
                        sender = ParaboxContact(ParaboxBasicInfo("S", ParaboxResourceInfo.ParaboxEmptyInfo), "s1"),
                        chat = message.chat,
                        timestamp = message.timestamp + 1,
                        uuid = "rx-${message.uuid}",
                    )
                )
                return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            }
        }
    }

    private fun startServer(serverName: String, provider: ParaboxExtensionProvider): Server {
        val engine = ParaboxExtensionGrpcEngine(listOf(provider))
        return InProcessServerBuilder.forName(serverName)
            .addService(engine)
            .build()
            .start()
    }

    @Test
    fun sessionClientReceivesMessagesAndAutoReconnectsAfterServerCrash() = runBlocking {
        val serverName = InProcessServerBuilder.generateName()
        val initCount = AtomicInteger(0)
        val provider = ReconnectTestProvider(initCount)
        var server = startServer(serverName, provider)

        val inProcTransport = InProcessExtensionTransport()
        val transport = object : ExtensionTransport {
            override fun acquireChannel(endpoint: ExtensionEndpoint): ManagedChannel =
                inProcTransport.acquireChannel(endpoint as ExtensionEndpoint.InProcess)
            override fun invalidateChannel(endpoint: ExtensionEndpoint) =
                inProcTransport.invalidateChannel(endpoint as ExtensionEndpoint.InProcess)
            override fun invalidatePackage(packageName: String) = Unit
            override fun shutdownAll() = inProcTransport.shutdownAll()
        }

        val receivedMessage = CompletableDeferred<ReceiveMessage>()
        val client = ExtensionSessionClient(
            sessionId = "conn-test-1",
            endpoint = ExtensionEndpoint.InProcess(serverName, "reconnect_test"),
            extra = JSONObject(),
            hostPackageName = "com.ojhdtapp.parabox",
            transport = transport,
            inboundHandler = object : SessionInboundHandler {
                override suspend fun onReceiveMessage(message: ReceiveMessage): ParaboxResult {
                    receivedMessage.complete(message)
                    return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
                }
                override suspend fun onRecallMessage(messageUuid: String): ParaboxResult =
                    ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            },
            ioDispatcher = Dispatchers.IO,
            initialBackoffMs = 100L,
            maxBackoffMs = 300L,
        )

        try {
            client.start()
            withTimeout(5_000) {
                client.status.first { it is ParaboxConnectionStatus.Active }
            }
            assertEquals(1, initCount.get())

            // Verify SendMessage -> ReceiveMessage roundtrip
            val sendRes = client.sendMessage(
                SendMessage(
                    contents = listOf(ParaboxPlainText("hello")),
                    chat = ParaboxChat(ParaboxBasicInfo("C", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "c1"),
                    timestamp = 100L,
                    uuid = "m1",
                )
            )
            assertEquals(ParaboxResult.SUCCESS, sendRes.code)
            val rx = withTimeout(5_000) { receivedMessage.await() }
            assertEquals("ack-from-server", (rx.contents.first() as ParaboxPlainText).text)

            // Simulate remote extension crash
            server.shutdownNow()
            server.awaitTermination(3, TimeUnit.SECONDS)

            withTimeout(5_000) {
                client.status.first { it is ParaboxConnectionStatus.Reconnecting }
            }

            // Bring extension service back up and verify automatic recovery
            server = startServer(serverName, provider)
            withTimeout(5_000) {
                client.status.first { it is ParaboxConnectionStatus.Active }
            }
            assertTrue("Expected session to re-initialize after reconnect", initCount.get() >= 2)
        } finally {
            client.close()
            transport.shutdownAll()
            server.shutdownNow()
        }
    }

    @Test
    fun sendMessageAutoRecoversAfterUninitializedAndInitFailureReconnects() = runBlocking {
        val serverName = InProcessServerBuilder.generateName()
        val initAttempts = AtomicInteger(0)
        val provider = object : ParaboxExtensionProvider {
            override val descriptor = ParaboxExtensionDescriptor(
                key = "recover_test",
                name = "Recover Test",
            )

            override suspend fun onInitStep(
                wizardSessionId: String,
                currentStepIndex: Int,
                submittedKey: String?,
                submittedValue: Any?,
                accumulatedExtra: JSONObject,
            ): ParaboxInitStepResult = ParaboxInitStepResult(emptyList(), 0, completed = true, updatedExtra = accumulatedExtra)

            override fun createSession(
                sessionId: String,
                extra: JSONObject,
                hostPackageName: String,
            ): ParaboxSession = object : ParaboxSession(sessionId, extra, hostPackageName) {
                override suspend fun onInitialize(): Boolean {
                    val attempt = initAttempts.incrementAndGet()
                    if (attempt == 1) {
                        // Fail first init to verify engine closes stream and client auto-reconnects
                        updateStatus(ParaboxConnectionStatus.Error("transient init failure"))
                        return false
                    }
                    updateStatus(ParaboxConnectionStatus.Active)
                    return true
                }

                override suspend fun onSendMessage(message: SendMessage): ParaboxResult {
                    return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG, messageUuid = "tg:100")
                }
            }
        }

        val engine = ParaboxExtensionGrpcEngine(listOf(provider))
        val server = InProcessServerBuilder.forName(serverName)
            .addService(engine)
            .build()
            .start()

        val inProcTransport = InProcessExtensionTransport()
        val transport = object : ExtensionTransport {
            override fun acquireChannel(endpoint: ExtensionEndpoint): ManagedChannel =
                inProcTransport.acquireChannel(endpoint as ExtensionEndpoint.InProcess)
            override fun invalidateChannel(endpoint: ExtensionEndpoint) =
                inProcTransport.invalidateChannel(endpoint as ExtensionEndpoint.InProcess)
            override fun invalidatePackage(packageName: String) = Unit
            override fun shutdownAll() = inProcTransport.shutdownAll()
        }

        val client = ExtensionSessionClient(
            sessionId = "conn-recover-1",
            endpoint = ExtensionEndpoint.InProcess(serverName, "recover_test"),
            extra = JSONObject(),
            hostPackageName = "com.ojhdtapp.parabox",
            transport = transport,
            inboundHandler = object : SessionInboundHandler {
                override suspend fun onReceiveMessage(message: ReceiveMessage) =
                    ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
                override suspend fun onRecallMessage(messageUuid: String) =
                    ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            },
            ioDispatcher = Dispatchers.IO,
            initialBackoffMs = 50L,
            maxBackoffMs = 200L,
            heartbeatIntervalMs = 500L,
            heartbeatTimeoutMs = 500L,
            recoverTimeoutMs = 5_000L,
        )

        try {
            client.start()
            // First init fails -> engine closes stream -> client reconnects -> 2nd init succeeds
            withTimeout(5_000) {
                client.status.first { it is ParaboxConnectionStatus.Active }
            }
            assertEquals(2, initAttempts.get())

            // Simulate remote activeSessions cleared while stream hasn't noticed yet
            engine.shutdownAllSessions()

            // sendMessage should receive ERROR_UNINITIALIZED, forceReconnect, and transparently succeed
            val res = client.sendMessage(
                SendMessage(
                    contents = listOf(ParaboxPlainText("recover me")),
                    chat = ParaboxChat(ParaboxBasicInfo("C", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "c1"),
                    timestamp = 200L,
                    uuid = "m-recover",
                )
            )
            assertEquals(ParaboxResult.SUCCESS, res.code)
            assertEquals("tg:100", res.messageUuid)
            assertTrue("Expected session to re-initialize on Uninitialized recovery", initAttempts.get() >= 3)
        } finally {
            client.close()
            transport.shutdownAll()
            server.shutdownNow()
        }
    }
}
