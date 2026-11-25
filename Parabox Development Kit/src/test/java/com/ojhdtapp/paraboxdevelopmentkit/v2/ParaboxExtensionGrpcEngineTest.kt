package com.ojhdtapp.paraboxdevelopmentkit.v2

import com.ojhdtapp.parabox.protocol.v2.BasicInfoRequest
import com.ojhdtapp.parabox.protocol.v2.GetConfigSchemaRequest
import com.ojhdtapp.parabox.protocol.v2.GetManifestRequest
import com.ojhdtapp.parabox.protocol.v2.HostSessionFrame
import com.ojhdtapp.parabox.protocol.v2.InitStepRequest
import com.ojhdtapp.parabox.protocol.v2.MessageAckFrame
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.parabox.protocol.v2.SendMessageRequest
import com.ojhdtapp.parabox.protocol.v2.SessionInitFrame
import com.ojhdtapp.parabox.protocol.v2.ValidateConfigRequest
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toProto
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class ParaboxExtensionGrpcEngineTest {

    private class FakeEchoProvider : ParaboxExtensionProvider {
        val closedFlag = AtomicBoolean(false)

        override val descriptor = ParaboxExtensionDescriptor(
            key = "fake_echo",
            name = "Fake Echo",
            description = "Test extension",
        )

        private val steps = listOf(
            ParaboxInitAction.TextInputAction(
                key = "token",
                title = "Enter Token",
                label = "Token",
            )
        )

        override suspend fun onInitStep(
            wizardSessionId: String,
            currentStepIndex: Int,
            submittedKey: String?,
            submittedValue: Any?,
            accumulatedExtra: JSONObject,
        ): ParaboxInitStepResult {
            if (currentStepIndex < 0) {
                return ParaboxInitStepResult(steps = steps, nextStepIndex = 0, updatedExtra = accumulatedExtra)
            }
            val token = submittedValue?.toString().orEmpty()
            if (token.isBlank()) {
                return ParaboxInitStepResult(
                    steps = steps,
                    nextStepIndex = currentStepIndex,
                    validationError = "Token cannot be empty",
                    updatedExtra = accumulatedExtra,
                )
            }
            accumulatedExtra.put("token", token)
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex + 1,
                completed = true,
                updatedExtra = accumulatedExtra,
            )
        }

        override suspend fun getConfigItems(currentExtra: JSONObject): List<ParaboxConfigItem> = listOf(
            ParaboxConfigItem.TextInputConfigItem("token", "Token", "Auth token", "Token")
        )

        override suspend fun validateConfig(
            configKey: String,
            value: Any?,
            currentExtra: JSONObject,
        ): ParaboxInitActionResult {
            return if (configKey == "token" && value?.toString().isNullOrBlank()) {
                ParaboxInitActionResult.Error("Token required")
            } else {
                ParaboxInitActionResult.Done
            }
        }

        override fun createSession(
            sessionId: String,
            extra: JSONObject,
            hostPackageName: String,
        ): ParaboxSession = object : ParaboxSession(sessionId, extra, hostPackageName) {
            override suspend fun onInitialize(): Boolean {
                updateStatus(ParaboxConnectionStatus.Active)
                return true
            }

            override suspend fun onSendMessage(message: SendMessage): ParaboxResult {
                val text = (message.contents.firstOrNull() as? ParaboxPlainText)?.text.orEmpty()
                val echoResult = emitMessage(
                    ReceiveMessage(
                        contents = listOf(ParaboxPlainText("echo:$text:${extra.optString("token")}")),
                        sender = ParaboxContact(
                            basicInfo = ParaboxBasicInfo("Bot", ParaboxResourceInfo.ParaboxEmptyInfo),
                            uid = "bot-1",
                        ),
                        chat = message.chat,
                        timestamp = message.timestamp + 1,
                        uuid = "echo-${message.uuid}",
                    )
                )
                return echoResult
            }

            override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo {
                return ParaboxBasicInfo("User-$userId", ParaboxResourceInfo.ParaboxEmptyInfo)
            }

            override suspend fun onClose() {
                closedFlag.set(true)
            }
        }
    }

    @Test
    fun fullPepV2LifecycleOverInProcessGrpc() = runBlocking {
        val provider = FakeEchoProvider()
        val engine = ParaboxExtensionGrpcEngine(listOf(provider), versionName = "2.0.0", versionCode = 2)
        val serverName = InProcessServerBuilder.generateName()
        val server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(engine)
            .build()
            .start()
        val channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build()

        try {
            val stub = ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)

            // 1. Manifest
            val manifest = stub.getManifest(
                GetManifestRequest.newBuilder()
                    .setHostProtocolVersion(ParaboxExtensionConstants.PROTOCOL_VERSION)
                    .setHostPackageName("com.ojhdtapp.parabox")
                    .build()
            )
            assertEquals(ParaboxExtensionConstants.PROTOCOL_VERSION, manifest.protocolVersion)
            assertEquals(1, manifest.extensionsCount)
            assertEquals("fake_echo", manifest.getExtensions(0).key)

            // 2. Wizard validation error & success
            val wizardId = UUID.randomUUID().toString()
            val step0 = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId(wizardId)
                    .setExtensionKey("fake_echo")
                    .setCurrentStepIndex(-1)
                    .build()
            )
            assertEquals(1, step0.stepsCount)
            assertFalse(step0.completed)

            val invalid = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId(wizardId)
                    .setExtensionKey("fake_echo")
                    .setCurrentStepIndex(0)
                    .setSubmittedStepKey("token")
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(""))
                    .build()
            )
            assertEquals("Token cannot be empty", invalid.validationError)
            assertFalse(invalid.completed)

            val valid = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId(wizardId)
                    .setExtensionKey("fake_echo")
                    .setCurrentStepIndex(0)
                    .setSubmittedStepKey("token")
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("secret123"))
                    .build()
            )
            assertTrue(valid.completed)
            assertEquals("secret123", JSONObject(valid.updatedExtraJson).getString("token"))

            // 3. Config schema & validation
            val schema = stub.getConfigSchema(
                GetConfigSchemaRequest.newBuilder()
                    .setExtensionKey("fake_echo")
                    .setCurrentExtraJson(valid.updatedExtraJson)
                    .build()
            )
            assertEquals(1, schema.itemsCount)
            val badConfig = stub.validateConfig(
                ValidateConfigRequest.newBuilder()
                    .setExtensionKey("fake_echo")
                    .setConfigKey("token")
                    .setValue(ParaboxProtoMapper.toConfigValueProto(""))
                    .build()
            )
            assertFalse(badConfig.valid)

            // 4. OpenSession bi-directional streaming + SendMessage + ReceiveMessage ACK
            val outbound = Channel<HostSessionFrame>(Channel.UNLIMITED)
            val activeDeferred = CompletableDeferred<Unit>()
            val receivedDeferred = CompletableDeferred<ReceiveMessage>()

            val streamJob = launch {
                stub.openSession(outbound.consumeAsFlow()).collect { frame ->
                    when (frame.payloadCase) {
                        com.ojhdtapp.parabox.protocol.v2.ExtensionSessionFrame.PayloadCase.STATUS_CHANGED -> {
                            if (frame.statusChanged.toDomain() is ParaboxConnectionStatus.Active) {
                                activeDeferred.complete(Unit)
                            }
                        }
                        com.ojhdtapp.parabox.protocol.v2.ExtensionSessionFrame.PayloadCase.RECEIVE_MESSAGE -> {
                            receivedDeferred.complete(frame.receiveMessage.message.toDomain())
                            outbound.send(
                                HostSessionFrame.newBuilder()
                                    .setAck(
                                        MessageAckFrame.newBuilder()
                                            .setAckId(frame.receiveMessage.ackId)
                                            .setResult(ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG).toProto())
                                            .build()
                                    )
                                    .build()
                            )
                        }
                        else -> Unit
                    }
                }
            }

            outbound.send(
                HostSessionFrame.newBuilder()
                    .setInit(
                        SessionInitFrame.newBuilder()
                            .setSessionId("conn-1")
                            .setExtensionKey("fake_echo")
                            .setExtraJson(valid.updatedExtraJson)
                            .setHostPackageName("com.ojhdtapp.parabox")
                            .build()
                    )
                    .build()
            )

            withTimeout(5_000) { activeDeferred.await() }

            val sendResp = stub.sendMessage(
                SendMessageRequest.newBuilder()
                    .setSessionId("conn-1")
                    .setMessage(
                        SendMessage(
                            contents = listOf(ParaboxPlainText("ping")),
                            chat = ParaboxChat(
                                basicInfo = ParaboxBasicInfo("Room", ParaboxResourceInfo.ParaboxEmptyInfo),
                                type = ParaboxChat.TYPE_PRIVATE,
                                uid = "room-1",
                            ),
                            timestamp = 1000L,
                            uuid = "m-1",
                        ).toProto()
                    )
                    .build()
            )
            assertEquals(ParaboxResult.SUCCESS, sendResp.code)

            val received = withTimeout(5_000) { receivedDeferred.await() }
            assertEquals("echo:ping:secret123", (received.contents.first() as ParaboxPlainText).text)

            val userInfo = stub.getUserBasicInfo(
                BasicInfoRequest.newBuilder().setSessionId("conn-1").setTargetId("42").build()
            )
            assertTrue(userInfo.found)
            assertEquals("User-42", userInfo.basicInfo.name)

            outbound.close()
            streamJob.join()
            assertTrue(provider.closedFlag.get())
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }
}
