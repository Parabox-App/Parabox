package com.ojhdt.parabox.extension.tdlib.mapper

import android.net.Uri
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.TdProxyProtocol
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TdMessageMapperTest {

    private val dummyClient = object : TdClientApi {
        override val accountId = "test"
        override val updates = MutableSharedFlow<TdApi.Object>()
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(null)
        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object = TdApi.Ok()
        override suspend fun applyProxy(proxyConfig: TdProxyConfig) = Unit
        override suspend fun closeAndWait(timeoutMs: Long) = Unit
    }

    private val dummyMediaBridge = object : TdMediaBridge {
        override suspend fun downloadAndShareFile(
            client: TdClientApi,
            tdFile: TdApi.File,
            hostPackageName: String,
            maxSizeBytes: Long,
            timeoutMs: Long,
        ): ParaboxResourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("file://mock/${tdFile.id}")

        override suspend fun stageResourceForUpload(
            accountId: String,
            resourceInfo: ParaboxResourceInfo,
            suggestedName: String?,
        ): File = File("/tmp/staged_${suggestedName ?: "file"}")
    }

    @Test
    fun proxyConfigParsingAndValidation() {
        val disabled = TdProxyConfig.fromExtra(JSONObject())
        assertFalse(disabled.enabled)
        assertTrue(disabled.isValid())

        val socks = TdProxyConfig.fromExtra(
            JSONObject().apply {
                put(TdProxyConfig.KEY_ENABLED, true)
                put(TdProxyConfig.KEY_TYPE, "SOCKS5")
                put(TdProxyConfig.KEY_HOST, "127.0.0.1")
                put(TdProxyConfig.KEY_PORT, "7890")
            }
        )
        assertTrue(socks.isValid())
        assertEquals(TdProxyProtocol.SOCKS5, socks.protocol)
        assertTrue(socks.toTdApiProxyType() is TdApi.ProxyTypeSocks5)

        val invalidMtproto = TdProxyConfig.fromExtra(
            JSONObject().apply {
                put(TdProxyConfig.KEY_ENABLED, true)
                put(TdProxyConfig.KEY_TYPE, "MTProto")
                put(TdProxyConfig.KEY_HOST, "proxy.example.com")
                put(TdProxyConfig.KEY_PORT, "443")
                put(TdProxyConfig.KEY_USERNAME, "")
            }
        )
        assertFalse(invalidMtproto.isValid())
    }

    @Test
    fun uuidAndChatTypeAndUserNameMapping() {
        val uuid = TdMessageMapper.formatMessageUuid(-100123456L, 98765L)
        assertEquals("-100123456:98765", uuid)
        assertEquals(-100123456L to 98765L, TdMessageMapper.parseMessageUuid(uuid))

        assertEquals(ParaboxChat.TYPE_PRIVATE, TdMessageMapper.mapChatType(TdApi.ChatTypePrivate(1L)))
        assertEquals(ParaboxChat.TYPE_GROUP, TdMessageMapper.mapChatType(TdApi.ChatTypeSupergroup(2L, false)))

        val user = TdApi.User().apply {
            id = 42L
            firstName = "Ada"
            lastName = "Lovelace"
        }
        assertEquals("Ada Lovelace", TdMessageMapper.formatUserDisplayName(user))
    }

    @Test
    fun inboundAndOutboundMessageMapping() = runBlocking {
        val tdMsg = TdApi.Message().apply {
            id = 555L
            chatId = -10010L
            date = 1700000000
            replyTo = TdApi.MessageReplyToMessage(-10010L, 550L, null, null, 1699999990, null)
            content = TdApi.MessageText(TdApi.FormattedText("Hello Telegram", emptyArray()), null, null)
        }
        val elements = TdMessageMapper.mapMessageContentToElements(
            message = tdMsg,
            client = dummyClient,
            mediaBridge = dummyMediaBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
        )
        assertEquals(2, elements.size)
        assertTrue(elements[0] is ParaboxQuoteReply)
        assertEquals("-10010:550", (elements[0] as ParaboxQuoteReply).id)
        assertEquals(ParaboxPlainText("Hello Telegram"), elements[1])

        // Outbound mapping: text + quote reply + location
        val send = SendMessage(
            contents = listOf(
                ParaboxQuoteReply(null, null, "-10010:555", emptyList()),
                ParaboxPlainText("Reply body"),
                ParaboxLocation(35.68, 139.76, "Tokyo", null),
            ),
            chat = ParaboxChat(ParaboxBasicInfo("Group", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_GROUP, "-10010"),
            timestamp = 1700000010L,
            uuid = "out-1",
        )
        val outbound = TdMessageMapper.mapSendMessageToOutbound(send, "acct-1", dummyMediaBridge)
        assertEquals(555L, outbound.replyToMessageId)
        assertEquals(2, outbound.contents.size)
        assertTrue(outbound.contents[0] is TdApi.InputMessageText)
        assertEquals("Reply body", (outbound.contents[0] as TdApi.InputMessageText).text.text)
        assertTrue(outbound.contents[1] is TdApi.InputMessageLocation)
    }

    @Test
    fun outboundPhotoAndTextMergeCaption() = runBlocking {
        val send = SendMessage(
            contents = listOf(
                ParaboxPlainText("Look at this"),
                ParaboxImage(100, 100, "pic.png", ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/pic.png")),
            ),
            chat = ParaboxChat(ParaboxBasicInfo("DM", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "1001"),
            timestamp = 1700000020L,
            uuid = "out-2",
        )
        val outbound = TdMessageMapper.mapSendMessageToOutbound(send, "acct-1", dummyMediaBridge)
        assertEquals(1, outbound.contents.size)
        val photo = outbound.contents[0] as TdApi.InputMessagePhoto
        assertEquals("Look at this", photo.caption.text)
    }
}
