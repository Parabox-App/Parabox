package com.ojhdt.parabox.extension.tdlib

import android.net.Uri
import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdErrorException
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.parabox.protocol.v2.ExecuteConfigActionRequest
import com.ojhdtapp.parabox.protocol.v2.ExtensionSessionFrame
import com.ojhdtapp.parabox.protocol.v2.HostSessionFrame
import com.ojhdtapp.parabox.protocol.v2.InitStepRequest
import com.ojhdtapp.parabox.protocol.v2.MessageAckFrame
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.parabox.protocol.v2.SendMessageRequest
import com.ojhdtapp.parabox.protocol.v2.SessionInitFrame
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toProto
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionGrpcEngine
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TdExtensionProviderMultiAccountTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class AccountBackendState(
        val requires2Fa: Boolean = false,
        var authorized: Boolean = false,
        var phone: String = "",
        var userId: Long = 100L,
        var displayName: String = "User",
    )

    private class FakeTdClient(
        override val accountId: String,
        val directories: TdAccountDirectories,
        val backend: AccountBackendState,
        val initialProxy: TdProxyConfig,
    ) : TdClientApi {
        override val updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 64)
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(
            if (backend.authorized) TdApi.AuthorizationStateReady() else TdApi.AuthorizationStateWaitPhoneNumber()
        )
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())

        val sentMessages = CopyOnWriteArrayList<Pair<Long, String>>()
        var appliedProxy: TdProxyConfig = initialProxy
        var closed: Boolean = false
        var loggedOut: Boolean = false

        init {
            // Simulate TDLib creating DB files inside this account's isolated directory
            File(directories.dbDir, "td.binlog").writeText("db-$accountId")
        }

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object {
            return when (function) {
                is TdApi.SetAuthenticationPhoneNumber -> {
                    backend.phone = function.phoneNumber
                    authorizationState.value = TdApi.AuthorizationStateWaitCode()
                    TdApi.Ok()
                }
                is TdApi.CheckAuthenticationCode -> {
                    if (function.code != "12345") {
                        throw TdErrorException(400, "PHONE_CODE_INVALID")
                    }
                    if (backend.requires2Fa) {
                        val waitPwd = TdApi.AuthorizationStateWaitPassword().apply {
                            passwordHint = "my-hint"
                        }
                        authorizationState.value = waitPwd
                    } else {
                        backend.authorized = true
                        authorizationState.value = TdApi.AuthorizationStateReady()
                    }
                    TdApi.Ok()
                }
                is TdApi.CheckAuthenticationPassword -> {
                    if (function.password != "pass2fa") {
                        throw TdErrorException(400, "PASSWORD_HASH_INVALID")
                    }
                    backend.authorized = true
                    authorizationState.value = TdApi.AuthorizationStateReady()
                    TdApi.Ok()
                }
                is TdApi.GetMe -> TdApi.User().apply {
                    id = backend.userId
                    firstName = backend.displayName
                    lastName = ""
                    phoneNumber = backend.phone
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = function.userId
                    firstName = "Peer-${function.userId}"
                    lastName = ""
                }
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = function.chatId
                    title = "Chat-${function.chatId}"
                    type = TdApi.ChatTypePrivate(function.chatId)
                }
                is TdApi.SendMessage -> {
                    val text = (function.inputMessageContent as? TdApi.InputMessageText)?.text?.text.orEmpty()
                    sentMessages.add(function.chatId to text)
                    TdApi.Message().apply {
                        id = 999L
                        chatId = function.chatId
                    }
                }
                is TdApi.LogOut -> {
                    loggedOut = true
                    backend.authorized = false
                    authorizationState.value = TdApi.AuthorizationStateClosed()
                    TdApi.Ok()
                }
                else -> TdApi.Ok()
            }
        }

        override suspend fun applyProxy(proxyConfig: TdProxyConfig) {
            appliedProxy = proxyConfig
        }

        override suspend fun closeAndWait(timeoutMs: Long) {
            closed = true
            authorizationState.value = TdApi.AuthorizationStateClosed()
        }

        fun pushIncomingText(chatId: Long, senderUserId: Long, msgId: Long, text: String) {
            val msg = TdApi.Message().apply {
                id = msgId
                this.chatId = chatId
                senderId = TdApi.MessageSenderUser(senderUserId)
                isOutgoing = false
                date = 1700000100
                content = TdApi.MessageText(TdApi.FormattedText(text, emptyArray()), null, null)
            }
            updates.tryEmit(TdApi.UpdateNewMessage(msg))
        }
    }

    private class FakeTdClientFactory : TdClientFactory {
        val backends = ConcurrentHashMap<String, AccountBackendState>()
        val createdHistory = ConcurrentHashMap<String, MutableList<FakeTdClient>>()
        var nextRequires2Fa: Boolean = false
        var nextUserId: Long = 1001L
        var nextDisplayName: String = "AccountA"

        override fun create(
            accountId: String,
            directories: TdAccountDirectories,
            apiId: Int,
            apiHash: String,
            proxyConfig: TdProxyConfig,
        ): TdClientApi {
            val backend = backends.computeIfAbsent(accountId) {
                AccountBackendState(
                    requires2Fa = nextRequires2Fa,
                    userId = nextUserId,
                    displayName = nextDisplayName,
                )
            }
            val client = FakeTdClient(accountId, directories, backend, proxyConfig)
            createdHistory.computeIfAbsent(accountId) { mutableListOf() }.add(client)
            return client
        }

        fun latestClient(accountId: String): FakeTdClient =
            createdHistory[accountId]!!.last()
    }

    private val noopMediaBridge = object : TdMediaBridge {
        override suspend fun downloadAndShareFile(
            client: TdClientApi,
            tdFile: TdApi.File,
            hostPackageName: String,
            maxSizeBytes: Long,
            timeoutMs: Long,
        ): ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo

        override suspend fun stageResourceForUpload(
            accountId: String,
            resourceInfo: ParaboxResourceInfo,
            suggestedName: String?,
        ): File? = null
    }

    @Test
    fun multiAccountOnboardingConcurrentSessionsAndIsolatedLogout() = runBlocking {
        val storage = TdAccountStorage(tempFolder.newFolder("td_root"))
        val factory = FakeTdClientFactory()
        val coordinator = TdClientCoordinator(storage, factory, apiId = 12345, apiHash = "hash")
        val provider = TdExtensionProvider(coordinator, noopMediaBridge, authWaitTimeoutMs = 5_000L)
        val engine = ParaboxExtensionGrpcEngine(listOf(provider))

        val serverName = InProcessServerBuilder.generateName()
        val server = InProcessServerBuilder.forName(serverName).addService(engine).build().start()
        val channel = InProcessChannelBuilder.forName(serverName).build()
        val stub = ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)

        try {
            // === Onboard Account A (No Proxy, No 2FA) ===
            factory.nextRequires2Fa = false
            factory.nextUserId = 1001L
            factory.nextDisplayName = "Alice"

            val aStep0 = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-a")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(-1)
                    .build()
            )
            var extraA = aStep0.updatedExtraJson

            val aProxy = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-a")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(0)
                    .setSubmittedStepKey(TdProxyConfig.KEY_ENABLED)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(false))
                    .setAccumulatedExtraJson(extraA)
                    .build()
            )
            extraA = aProxy.updatedExtraJson

            val aPhone = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-a")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(aProxy.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_PHONE_NUMBER)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("+8613800000001"))
                    .setAccumulatedExtraJson(extraA)
                    .build()
            )
            assertEquals("", aPhone.validationError)
            extraA = aPhone.updatedExtraJson

            val aCode = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-a")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(aPhone.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_AUTH_CODE)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("12345"))
                    .setAccumulatedExtraJson(extraA)
                    .build()
            )
            assertTrue("Account A without 2FA should complete immediately", aCode.completed)
            extraA = aCode.updatedExtraJson
            val accountIdA = JSONObject(extraA).getString(TdExtensionProvider.KEY_ACCOUNT_ID)
            assertTrue("Wizard client for A must be closed before session handoff", factory.latestClient(accountIdA).closed)

            // === Onboard Account B (SOCKS5 Proxy + 2FA) ===
            factory.nextRequires2Fa = true
            factory.nextUserId = 2002L
            factory.nextDisplayName = "Bob"

            val bStep0 = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(-1)
                    .build()
            )
            var extraB = bStep0.updatedExtraJson

            val bProxyEnable = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(0)
                    .setSubmittedStepKey(TdProxyConfig.KEY_ENABLED)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(true))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyEnable.updatedExtraJson
            assertTrue("Enabling proxy should expand steps", bProxyEnable.stepsCount > aProxy.stepsCount)

            val bProxyType = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyEnable.nextStepIndex)
                    .setSubmittedStepKey(TdProxyConfig.KEY_TYPE)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(0))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyType.updatedExtraJson

            val bProxyHost = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyType.nextStepIndex)
                    .setSubmittedStepKey(TdProxyConfig.KEY_HOST)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("127.0.0.1"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyHost.updatedExtraJson

            val bProxyPort = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyHost.nextStepIndex)
                    .setSubmittedStepKey(TdProxyConfig.KEY_PORT)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("1080"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyPort.updatedExtraJson

            val bProxyUser = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyPort.nextStepIndex)
                    .setSubmittedStepKey(TdProxyConfig.KEY_USERNAME)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(""))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyUser.updatedExtraJson

            val bProxyPass = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyUser.nextStepIndex)
                    .setSubmittedStepKey(TdProxyConfig.KEY_PASSWORD)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto(""))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bProxyPass.updatedExtraJson

            val bPhone = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bProxyPass.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_PHONE_NUMBER)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("+8613800000002"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            extraB = bPhone.updatedExtraJson

            val bCode = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bPhone.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_AUTH_CODE)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("12345"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            assertFalse("Account B requires 2FA password step", bCode.completed)
            assertTrue(bCode.stepsList.any { it.key == TdExtensionProvider.KEY_TWO_FA_PASSWORD && it.description.contains("my-hint") })
            extraB = bCode.updatedExtraJson

            // Wrong 2FA password returns inline validation error
            val bWrongPwd = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bCode.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_TWO_FA_PASSWORD)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("wrong"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            assertFalse(bWrongPwd.completed)
            assertTrue(bWrongPwd.validationError.contains("PASSWORD_HASH_INVALID"))

            // Correct 2FA password completes onboarding
            val bPwdOk = stub.nextInitStep(
                InitStepRequest.newBuilder()
                    .setWizardSessionId("wiz-b")
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setCurrentStepIndex(bCode.nextStepIndex)
                    .setSubmittedStepKey(TdExtensionProvider.KEY_TWO_FA_PASSWORD)
                    .setSubmittedValue(ParaboxProtoMapper.toConfigValueProto("pass2fa"))
                    .setAccumulatedExtraJson(extraB)
                    .build()
            )
            assertTrue(bPwdOk.completed)
            extraB = bPwdOk.updatedExtraJson
            val accountIdB = JSONObject(extraB).getString(TdExtensionProvider.KEY_ACCOUNT_ID)
            assertNotEquals("Each connection must have an isolated account_id", accountIdA, accountIdB)
            assertTrue(factory.latestClient(accountIdB).closed)
            assertEquals(1080, factory.latestClient(accountIdB).initialProxy.port)

            // === Open BOTH Account A & Account B Sessions Concurrently ===
            val outA = Channel<HostSessionFrame>(Channel.UNLIMITED)
            val outB = Channel<HostSessionFrame>(Channel.UNLIMITED)
            val activeA = CompletableDeferred<Unit>()
            val activeB = CompletableDeferred<Unit>()
            val rxA = CompletableDeferred<ReceiveMessage>()
            val rxB = CompletableDeferred<ReceiveMessage>()

            val jobA = launch {
                stub.openSession(outA.consumeAsFlow()).collect { frame ->
                    when (frame.payloadCase) {
                        ExtensionSessionFrame.PayloadCase.STATUS_CHANGED -> {
                            if (frame.statusChanged.toDomain() is ParaboxConnectionStatus.Active) activeA.complete(Unit)
                        }
                        ExtensionSessionFrame.PayloadCase.RECEIVE_MESSAGE -> {
                            rxA.complete(frame.receiveMessage.message.toDomain())
                            outA.send(
                                HostSessionFrame.newBuilder().setAck(
                                    MessageAckFrame.newBuilder()
                                        .setAckId(frame.receiveMessage.ackId)
                                        .setResult(ParaboxResult(ParaboxResult.SUCCESS, "ok").toProto())
                                ).build()
                            )
                        }
                        else -> Unit
                    }
                }
            }
            val jobB = launch {
                stub.openSession(outB.consumeAsFlow()).collect { frame ->
                    when (frame.payloadCase) {
                        ExtensionSessionFrame.PayloadCase.STATUS_CHANGED -> {
                            if (frame.statusChanged.toDomain() is ParaboxConnectionStatus.Active) activeB.complete(Unit)
                        }
                        ExtensionSessionFrame.PayloadCase.RECEIVE_MESSAGE -> {
                            rxB.complete(frame.receiveMessage.message.toDomain())
                            outB.send(
                                HostSessionFrame.newBuilder().setAck(
                                    MessageAckFrame.newBuilder()
                                        .setAckId(frame.receiveMessage.ackId)
                                        .setResult(ParaboxResult(ParaboxResult.SUCCESS, "ok").toProto())
                                ).build()
                            )
                        }
                        else -> Unit
                    }
                }
            }

            outA.send(
                HostSessionFrame.newBuilder().setInit(
                    SessionInitFrame.newBuilder()
                        .setSessionId("conn-A")
                        .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                        .setExtraJson(extraA)
                        .setHostPackageName("com.ojhdtapp.parabox")
                ).build()
            )
            outB.send(
                HostSessionFrame.newBuilder().setInit(
                    SessionInitFrame.newBuilder()
                        .setSessionId("conn-B")
                        .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                        .setExtraJson(extraB)
                        .setHostPackageName("com.ojhdtapp.parabox")
                ).build()
            )

            withTimeout(5_000) {
                activeA.await()
                activeB.await()
            }

            // Verify isolated inbound messages on A and B simultaneously
            val sessionClientA = factory.latestClient(accountIdA)
            val sessionClientB = factory.latestClient(accountIdB)
            sessionClientA.pushIncomingText(chatId = 5001L, senderUserId = 8001L, msgId = 11L, text = "msg-for-A")
            sessionClientB.pushIncomingText(chatId = 5001L, senderUserId = 8001L, msgId = 22L, text = "msg-for-B")

            assertEquals("msg-for-A", (withTimeout(5_000) { rxA.await() }.contents.first() as ParaboxPlainText).text)
            assertEquals("msg-for-B", (withTimeout(5_000) { rxB.await() }.contents.first() as ParaboxPlainText).text)

            // Verify isolated outbound SendMessage
            stub.sendMessage(
                SendMessageRequest.newBuilder()
                    .setSessionId("conn-A")
                    .setMessage(
                        SendMessage(
                            contents = listOf(ParaboxPlainText("from-A")),
                            chat = ParaboxChat(ParaboxBasicInfo("C", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "5001"),
                            timestamp = 1L,
                            uuid = "u1",
                        ).toProto()
                    )
                    .build()
            )
            stub.sendMessage(
                SendMessageRequest.newBuilder()
                    .setSessionId("conn-B")
                    .setMessage(
                        SendMessage(
                            contents = listOf(ParaboxPlainText("from-B")),
                            chat = ParaboxChat(ParaboxBasicInfo("C", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "5001"),
                            timestamp = 2L,
                            uuid = "u2",
                        ).toProto()
                    )
                    .build()
            )
            assertEquals(listOf(5001L to "from-A"), sessionClientA.sentMessages.toList())
            assertEquals(listOf(5001L to "from-B"), sessionClientB.sentMessages.toList())

            // Logout Account A ONLY -> Account B must stay active and untouched!
            val logoutRes = stub.executeConfigAction(
                ExecuteConfigActionRequest.newBuilder()
                    .setExtensionKey(TdExtensionProvider.EXTENSION_KEY)
                    .setSessionId("conn-A")
                    .setActionKey(TdExtensionProvider.ACTION_LOGOUT)
                    .setCurrentExtraJson(extraA)
                    .build()
            )
            assertEquals(ParaboxResult.SUCCESS, logoutRes.code)
            assertTrue(sessionClientA.loggedOut)
            assertFalse(sessionClientB.loggedOut)
            assertFalse(coordinator.isAccountActive(accountIdA))
            assertTrue(coordinator.isAccountActive(accountIdB))
            assertEquals(1, storage.listAccounts().size)
            assertEquals(accountIdB, storage.listAccounts().first().accountId)

            outA.close()
            outB.close()
            jobA.join()
            jobB.join()
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
            coordinator.shutdownAll()
        }
    }
}
