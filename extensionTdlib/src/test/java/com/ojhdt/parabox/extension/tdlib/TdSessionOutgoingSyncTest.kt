package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TdSessionOutgoingSyncTest {

    private class FakeSyncTdClient(
        override val accountId: String = "acct-sync",
        private val onFunction: (TdApi.Function<*>) -> TdApi.Object,
    ) : TdClientApi {
        override val updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 64)
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(TdApi.AuthorizationStateReady())
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object = when (function) {
            is TdApi.GetMe -> TdApi.User().apply {
                id = 100L
                firstName = "Me"
            }
            is TdApi.GetChat -> TdApi.Chat().apply {
                id = function.chatId
                title = if (function.chatId == 200L) "Alice" else "Group"
                type = if (function.chatId == 200L) TdApi.ChatTypePrivate(200L) else TdApi.ChatTypeSupergroup(1L, false)
            }
            is TdApi.GetUser -> TdApi.User().apply {
                id = function.userId
                firstName = if (function.userId == 100L) "Me" else "Alice"
            }
            else -> onFunction(function)
        }

        override suspend fun applyProxy(proxyConfig: TdProxyConfig) = Unit
        override suspend fun closeAndWait(timeoutMs: Long) = Unit
    }

    private val stubMediaBridge = object : TdMediaBridge {
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

    private fun createSession(client: TdClientApi): TdSession {
        val root = Files.createTempDirectory("td_sync_test_").toFile()
        val storage = TdAccountStorage(root)
        val factory = object : TdClientFactory {
            override fun create(
                accountId: String,
                directories: TdAccountDirectories,
                apiId: Int,
                apiHash: String,
                proxyConfig: TdProxyConfig,
            ): TdClientApi = client
        }
        val coordinator = TdClientCoordinator(storage, factory, 12345, "hash")
        return TdSession(
            sessionId = "conn-1",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, client.accountId),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = coordinator,
            mediaBridge = stubMediaBridge,
        )
    }

    private fun attachTestScope(session: TdSession): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val field = ParaboxSession::class.java.getDeclaredField("_sessionScope")
        field.isAccessible = true
        field.set(session, scope)
        return scope
    }

    @Test
    fun otherClientOutgoingMessageIsSyncedAndMarkedSentByMe() = runBlocking {
        val client = FakeSyncTdClient { TdApi.Ok() }
        val session = createSession(client)

        val remoteOutgoing = TdApi.Message().apply {
            id = 8001L
            chatId = 200L
            date = 1700001000
            isOutgoing = true
            sendingState = null
            senderId = TdApi.MessageSenderUser(100L)
            content = TdApi.MessageText(TdApi.FormattedText("From Desktop", emptyArray()), null, null)
        }

        assertTrue(session.shouldSyncUpdateNewMessage(remoteOutgoing))
        val built = session.buildReceiveMessage(client, remoteOutgoing)
        assertNotNull(built)
        assertTrue(built!!.sentByMe)
        assertEquals("200:8001", built.uuid)
        assertEquals("200", built.chat.uid)
        assertEquals(listOf(ParaboxPlainText("From Desktop")), built.contents)
    }

    @Test
    fun pendingOrLocallySentOutgoingMessagesAreIgnoredToPreventDuplicates() = runBlocking {
        val client = FakeSyncTdClient { fn ->
            if (fn is TdApi.SendMessage) {
                TdApi.Message().apply {
                    id = 55L
                    chatId = fn.chatId
                    isOutgoing = true
                    sendingState = TdApi.MessageSendingStatePending(0)
                }
            } else {
                TdApi.Ok()
            }
        }
        val session = createSession(client)
        val scope = attachTestScope(session)
        try {
            assertTrue(session.onInitialize())

            // Pending local send must never be synced
            val pendingMsg = TdApi.Message().apply {
                id = 55L
                chatId = 200L
                isOutgoing = true
                sendingState = TdApi.MessageSendingStatePending(0)
            }
            assertFalse(session.shouldSyncUpdateNewMessage(pendingMsg))

            // Perform a local send and confirm with UpdateMessageSendSucceeded -> finalId = 9001L
            val sendDeferred = async {
                session.onSendMessage(
                    SendMessage(
                        contents = listOf(ParaboxPlainText("Hello locally")),
                        chat = ParaboxChat(
                            ParaboxBasicInfo("Alice", ParaboxResourceInfo.ParaboxEmptyInfo),
                            ParaboxChat.TYPE_PRIVATE,
                            "200",
                        ),
                        timestamp = 1700002000L,
                        uuid = "local-uuid-1",
                    )
                )
            }
            delay(30)
            client.updates.emit(
                TdApi.UpdateMessageSendSucceeded(
                    TdApi.Message().apply {
                        id = 9001L
                        chatId = 200L
                        isOutgoing = true
                        sendingState = null
                    },
                    55L,
                )
            )
            val result = sendDeferred.await()
            assertEquals("200:9001", result.messageUuid)

            // Echo with finalId = 9001L must be suppressed
            val echoedFinal = TdApi.Message().apply {
                id = 9001L
                chatId = 200L
                isOutgoing = true
                sendingState = null
            }
            assertFalse(session.shouldSyncUpdateNewMessage(echoedFinal))

            // Unrelated outgoing message from another client is still synced
            val otherClientMsg = TdApi.Message().apply {
                id = 9002L
                chatId = 200L
                isOutgoing = true
                sendingState = null
            }
            assertTrue(session.shouldSyncUpdateNewMessage(otherClientMsg))
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun incomingMessageFromPeerHasSentByMeFalse() = runBlocking {
        val client = FakeSyncTdClient { TdApi.Ok() }
        val session = createSession(client)

        val peerMsg = TdApi.Message().apply {
            id = 8002L
            chatId = 200L
            date = 1700001100
            isOutgoing = false
            sendingState = null
            senderId = TdApi.MessageSenderUser(200L)
            content = TdApi.MessageText(TdApi.FormattedText("Hi from Alice", emptyArray()), null, null)
        }

        assertTrue(session.shouldSyncUpdateNewMessage(peerMsg))
        val built = session.buildReceiveMessage(client, peerMsg)
        assertNotNull(built)
        assertFalse(built!!.sentByMe)
    }
}
