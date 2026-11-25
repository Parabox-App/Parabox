package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TdSessionSendMessageTest {

    private class FakeSendTdClient(
        override val accountId: String,
        private val onSendFunction: (TdApi.Function<*>) -> TdApi.Object,
    ) : TdClientApi {
        override val updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 64)
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(TdApi.AuthorizationStateReady())
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object {
            return when (function) {
                is TdApi.GetMe -> TdApi.User().apply {
                    id = 100L
                    firstName = "Me"
                }
                else -> onSendFunction(function)
            }
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
        ): File = Files.createTempFile("td_test_", ".jpg").toFile()
    }

    private fun createCoordinator(client: TdClientApi): TdClientCoordinator {
        val root = Files.createTempDirectory("td_send_test_").toFile()
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
        return TdClientCoordinator(storage, factory, 12345, "hash")
    }

    private fun attachTestScope(session: TdSession): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val field = ParaboxSession::class.java.getDeclaredField("_sessionScope")
        field.isAccessible = true
        field.set(session, scope)
        return scope
    }

    private fun textSendMessage(chatId: String = "-100999"): SendMessage = SendMessage(
        contents = listOf(ParaboxPlainText("hello")),
        chat = ParaboxChat(ParaboxBasicInfo("Test", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_GROUP, chatId),
        timestamp = 1700000000L,
        uuid = "local-1",
    )

    @Test
    fun onSendMessageReturnsRealUuidOnAsyncSucceeded() = runBlocking {
        val fakeClient = FakeSendTdClient("acct-1") { fn ->
            if (fn is TdApi.SendMessage) {
                TdApi.Message().apply {
                    id = 10L
                    chatId = fn.chatId
                    sendingState = TdApi.MessageSendingStatePending(0)
                }
            } else {
                TdApi.Ok()
            }
        }
        val session = TdSession(
            sessionId = "s1",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-1"),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = createCoordinator(fakeClient),
            mediaBridge = stubMediaBridge,
            sendAckTimeoutMs = 2_000L,
        )
        val scope = attachTestScope(session)
        try {
            assertTrue(session.onInitialize())

            val deferred = async { session.onSendMessage(textSendMessage("-100999")) }
            delay(50)
            fakeClient.updates.emit(
                TdApi.UpdateMessageSendSucceeded(
                    TdApi.Message().apply {
                        id = 987654L
                        chatId = -100999L
                    },
                    10L,
                )
            )

            val result = deferred.await()
            assertEquals(ParaboxResult.SUCCESS, result.code)
            assertEquals("-100999:987654", result.messageUuid)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun onSendMessageReturnsFormattedErrorOnAsyncFailed() = runBlocking {
        val fakeClient = FakeSendTdClient("acct-2") { fn ->
            if (fn is TdApi.SendMessage) {
                TdApi.Message().apply {
                    id = 20L
                    chatId = fn.chatId
                    sendingState = TdApi.MessageSendingStatePending(0)
                }
            } else {
                TdApi.Ok()
            }
        }
        val session = TdSession(
            sessionId = "s2",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-2"),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = createCoordinator(fakeClient),
            mediaBridge = stubMediaBridge,
            sendAckTimeoutMs = 2_000L,
        )
        val scope = attachTestScope(session)
        try {
            assertTrue(session.onInitialize())

            val deferred = async { session.onSendMessage(textSendMessage("-100999")) }
            delay(50)
            fakeClient.updates.emit(
                TdApi.UpdateMessageSendFailed(
                    TdApi.Message().apply {
                        id = 20L
                        chatId = -100999L
                    },
                    20L,
                    TdApi.Error(403, "CHAT_WRITE_FORBIDDEN"),
                )
            )

            val result = deferred.await()
            assertEquals(ParaboxResult.ERROR_UNKNOWN, result.code)
            assertTrue(result.message.contains("无权在此会话发送消息"))
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun onSendMessageReturnsTimeoutWhenNoServerAck() = runBlocking {
        val fakeClient = FakeSendTdClient("acct-3") { fn ->
            if (fn is TdApi.SendMessage) {
                TdApi.Message().apply {
                    id = 30L
                    chatId = fn.chatId
                    sendingState = TdApi.MessageSendingStatePending(0)
                }
            } else {
                TdApi.Ok()
            }
        }
        val session = TdSession(
            sessionId = "s3",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-3"),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = createCoordinator(fakeClient),
            mediaBridge = stubMediaBridge,
            sendAckTimeoutMs = 200L,
        )
        val scope = attachTestScope(session)
        try {
            assertTrue(session.onInitialize())

            val result = session.onSendMessage(textSendMessage("-100999"))
            assertEquals(ParaboxResult.ERROR_TIMEOUT, result.code)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun onSendMessageUsesSendMessageAlbumForMultiplePhotos() = runBlocking {
        var usedAlbum = false
        val fakeClient = FakeSendTdClient("acct-4") { fn ->
            if (fn is TdApi.SendMessageAlbum) {
                usedAlbum = true
                TdApi.Messages(
                    2,
                    arrayOf(
                        TdApi.Message().apply {
                            id = 41L
                            chatId = fn.chatId
                            sendingState = TdApi.MessageSendingStatePending(0)
                        },
                        TdApi.Message().apply {
                            id = 42L
                            chatId = fn.chatId
                            sendingState = TdApi.MessageSendingStatePending(0)
                        },
                    ),
                )
            } else {
                TdApi.Ok()
            }
        }
        val session = TdSession(
            sessionId = "s4",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-4"),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = createCoordinator(fakeClient),
            mediaBridge = stubMediaBridge,
            sendAckTimeoutMs = 2_000L,
        )
        val scope = attachTestScope(session)
        try {
            assertTrue(session.onInitialize())

            val albumSend = SendMessage(
                contents = listOf(
                    ParaboxImage(10, 10, "1.jpg", ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/1.jpg")),
                    ParaboxImage(10, 10, "2.jpg", ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/2.jpg")),
                ),
                chat = ParaboxChat(ParaboxBasicInfo("Group", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_GROUP, "-100999"),
                timestamp = 1700000000L,
                uuid = "album-1",
            )

            val deferred = async { session.onSendMessage(albumSend) }
            delay(50)
            fakeClient.updates.emit(
                TdApi.UpdateMessageSendSucceeded(
                    TdApi.Message().apply { id = 5001L; chatId = -100999L },
                    41L,
                )
            )
            fakeClient.updates.emit(
                TdApi.UpdateMessageSendSucceeded(
                    TdApi.Message().apply { id = 5002L; chatId = -100999L },
                    42L,
                )
            )

            val result = deferred.await()
            assertTrue(usedAlbum)
            assertEquals(ParaboxResult.SUCCESS, result.code)
            assertEquals("-100999:5002", result.messageUuid)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }
}
