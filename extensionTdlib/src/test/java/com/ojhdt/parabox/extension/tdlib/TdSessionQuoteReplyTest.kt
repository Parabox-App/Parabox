package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class TdSessionQuoteReplyTest {

    private class FakeQuoteTdClient(
        override val accountId: String = "acct-quote",
        private val handler: (TdApi.Function<*>) -> TdApi.Object,
    ) : TdClientApi {
        override val updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 32)
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(TdApi.AuthorizationStateReady())
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object = handler(function)
        override suspend fun applyProxy(proxyConfig: TdProxyConfig) = Unit
        override suspend fun closeAndWait(timeoutMs: Long) = Unit
    }

    private fun createSession(
        client: TdClientApi,
        mediaDownloadCounter: AtomicInteger = AtomicInteger(0),
    ): TdSession {
        val root = Files.createTempDirectory("td_quote_test_").toFile()
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
        val mediaBridge = object : TdMediaBridge {
            override suspend fun downloadAndShareFile(
                client: TdClientApi,
                tdFile: TdApi.File,
                hostPackageName: String,
                maxSizeBytes: Long,
                timeoutMs: Long,
            ): ParaboxResourceInfo {
                mediaDownloadCounter.incrementAndGet()
                return ParaboxResourceInfo.ParaboxEmptyInfo
            }

            override suspend fun stageResourceForUpload(
                accountId: String,
                resourceInfo: ParaboxResourceInfo,
                suggestedName: String?,
            ): File? = null
        }
        return TdSession(
            sessionId = "s-quote",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, client.accountId),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = coordinator,
            mediaBridge = mediaBridge,
        )
    }

    @Test
    fun sameChatPlainReplyResolvesOriginalSenderAndContent() = runBlocking {
        val getRepliedCalls = AtomicInteger(0)
        val client = FakeQuoteTdClient { fn ->
            when (fn) {
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = fn.chatId
                    title = "Dev Group"
                    type = TdApi.ChatTypeSupergroup(1L, false)
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = fn.userId
                    firstName = if (fn.userId == 10L) "Alice" else "Bob"
                }
                is TdApi.GetRepliedMessage -> {
                    getRepliedCalls.incrementAndGet()
                    TdApi.Message().apply {
                        id = 500L
                        chatId = -10010L
                        date = 1700000100
                        senderId = TdApi.MessageSenderUser(10L)
                        // Nested reply on the original message must NOT trigger recursive fetch
                        replyTo = TdApi.MessageReplyToMessage(-10010L, 400L, null, null, 0, null)
                        content = TdApi.MessageText(TdApi.FormattedText("Original question", emptyArray()), null, null)
                    }
                }
                else -> TdApi.Error(404, "Not Found")
            }
        }
        val session = createSession(client)

        val incoming = TdApi.Message().apply {
            id = 501L
            chatId = -10010L
            date = 1700000200
            senderId = TdApi.MessageSenderUser(20L)
            replyTo = TdApi.MessageReplyToMessage(-10010L, 500L, null, null, 0, null)
            content = TdApi.MessageText(TdApi.FormattedText("Here is the answer", emptyArray()), null, null)
        }

        val received = session.buildReceiveMessage(client, incoming)
        assertNotNull(received)
        val quote = received!!.contents.filterIsInstance<ParaboxQuoteReply>().single()
        assertEquals("-10010:500", quote.id)
        assertEquals("Alice", quote.sender?.basicInfo?.name)
        assertEquals("10", quote.sender?.uid)
        assertEquals(1700000100L * 1000L, quote.timestamp)
        assertEquals(listOf(ParaboxPlainText("Original question")), quote.messages)
        assertEquals(1, getRepliedCalls.get())
    }

    @Test
    fun partialQuotePrefersSelectedQuoteTextWhileKeepingOriginalSender() = runBlocking {
        val client = FakeQuoteTdClient { fn ->
            when (fn) {
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = fn.chatId
                    title = "DM"
                    type = TdApi.ChatTypePrivate(10L)
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = fn.userId
                    firstName = "Alice"
                }
                is TdApi.GetRepliedMessage -> TdApi.Message().apply {
                    id = 600L
                    chatId = 10L
                    date = 1700000300
                    senderId = TdApi.MessageSenderUser(10L)
                    content = TdApi.MessageText(TdApi.FormattedText("A very long original message body", emptyArray()), null, null)
                }
                else -> TdApi.Error(404, "Not Found")
            }
        }
        val session = createSession(client)

        val incoming = TdApi.Message().apply {
            id = 601L
            chatId = 10L
            date = 1700000350
            senderId = TdApi.MessageSenderUser(10L)
            replyTo = TdApi.MessageReplyToMessage(
                10L,
                600L,
                TdApi.TextQuote(TdApi.FormattedText("original message", emptyArray()), 12, true),
                null,
                0,
                null,
            )
            content = TdApi.MessageText(TdApi.FormattedText("Agreed!", emptyArray()), null, null)
        }

        val received = session.buildReceiveMessage(client, incoming)
        val quote = received!!.contents.filterIsInstance<ParaboxQuoteReply>().single()
        assertEquals("Alice", quote.sender?.basicInfo?.name)
        assertEquals("10:600", quote.id)
        assertEquals(listOf(ParaboxPlainText("original message")), quote.messages)
    }

    @Test
    fun replyToPhotoMessageDoesNotDownloadMediaForPreview() = runBlocking {
        val downloadCounter = AtomicInteger(0)
        val client = FakeQuoteTdClient { fn ->
            when (fn) {
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = fn.chatId
                    title = "Photos"
                    type = TdApi.ChatTypeSupergroup(2L, false)
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = fn.userId
                    firstName = "Carol"
                }
                is TdApi.GetRepliedMessage -> TdApi.Message().apply {
                    id = 700L
                    chatId = -10020L
                    date = 1700000400
                    senderId = TdApi.MessageSenderUser(30L)
                    content = TdApi.MessagePhoto(
                        TdApi.Photo().apply {
                            sizes = arrayOf(
                                TdApi.PhotoSize().apply {
                                    width = 1280
                                    height = 720
                                    photo = TdApi.File().apply { id = 999 }
                                }
                            )
                        },
                        TdApi.FormattedText("Trip photo", emptyArray()),
                        false,
                        false,
                        false,
                    )
                }
                else -> TdApi.Error(404, "Not Found")
            }
        }
        val session = createSession(client, downloadCounter)

        val incoming = TdApi.Message().apply {
            id = 701L
            chatId = -10020L
            date = 1700000450
            senderId = TdApi.MessageSenderUser(30L)
            replyTo = TdApi.MessageReplyToMessage(-10020L, 700L, null, null, 0, null)
            content = TdApi.MessageText(TdApi.FormattedText("Nice shot", emptyArray()), null, null)
        }

        val received = session.buildReceiveMessage(client, incoming)
        val quote = received!!.contents.filterIsInstance<ParaboxQuoteReply>().single()
        assertEquals("Carol", quote.sender?.basicInfo?.name)
        assertEquals(2, quote.messages.size)
        assertTrue(quote.messages[0] is ParaboxImage)
        assertEquals(ParaboxResourceInfo.ParaboxEmptyInfo, (quote.messages[0] as ParaboxImage).resourceInfo)
        assertEquals(ParaboxPlainText("Trip photo"), quote.messages[1])
        assertEquals(0, downloadCounter.get())
    }

    @Test
    fun deletedOrExternalReplyFallsBackToOriginAndContent() = runBlocking {
        val client = FakeQuoteTdClient { fn ->
            when (fn) {
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = fn.chatId
                    title = if (fn.chatId == -10099L) "News Channel" else "Chat"
                    type = TdApi.ChatTypeSupergroup(3L, false)
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = fn.userId
                    firstName = "Dave"
                }
                else -> TdApi.Error(404, "Message not found")
            }
        }
        val session = createSession(client)

        val incoming = TdApi.Message().apply {
            id = 801L
            chatId = -10030L
            date = 1700000600
            senderId = TdApi.MessageSenderUser(40L)
            replyTo = TdApi.MessageReplyToMessage(
                0L,
                0L,
                null,
                TdApi.MessageOriginChannel(-10099L, 123L, "Editor"),
                1700000500,
                TdApi.MessageText(TdApi.FormattedText("Breaking update", emptyArray()), null, null),
            )
            content = TdApi.MessageText(TdApi.FormattedText("Check this out", emptyArray()), null, null)
        }

        val received = session.buildReceiveMessage(client, incoming)
        val quote = received!!.contents.filterIsInstance<ParaboxQuoteReply>().single()
        assertNull(quote.id)
        assertEquals("News Channel (Editor)", quote.sender?.basicInfo?.name)
        assertEquals("-10099", quote.sender?.uid)
        assertEquals(1700000500L * 1000L, quote.timestamp)
        assertEquals(listOf(ParaboxPlainText("Breaking update")), quote.messages)
    }
}
