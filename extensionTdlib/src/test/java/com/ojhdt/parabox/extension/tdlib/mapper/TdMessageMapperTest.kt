package com.ojhdt.parabox.extension.tdlib.mapper

import android.net.Uri
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.TdProxyProtocol
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
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

    @Test
    fun outboundMentionAndMultiPhotoAlbumAndAudioVideoFile() = runBlocking {
        val chat = ParaboxChat(ParaboxBasicInfo("Group", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_GROUP, "-10010")

        // @mention -> TextEntityTypeMentionName
        val mentionSend = SendMessage(
            contents = listOf(
                ParaboxAt(ParaboxContact(ParaboxBasicInfo("Alice", ParaboxResourceInfo.ParaboxEmptyInfo), "42")),
                ParaboxPlainText("hello"),
            ),
            chat = chat,
            timestamp = 1700000030L,
            uuid = "out-3",
        )
        val mentionOutbound = TdMessageMapper.mapSendMessageToOutbound(mentionSend, "acct-1", dummyMediaBridge)
        assertEquals(1, mentionOutbound.contents.size)
        val textMsg = mentionOutbound.contents[0] as TdApi.InputMessageText
        assertEquals("@Alice hello", textMsg.text.text)
        assertEquals(1, textMsg.text.entities.size)
        val mentionEntity = textMsg.text.entities[0]
        assertEquals(0, mentionEntity.offset)
        assertEquals("@Alice".length, mentionEntity.length)
        assertTrue(mentionEntity.type is TdApi.TextEntityTypeMentionName)
        assertEquals(42L, (mentionEntity.type as TdApi.TextEntityTypeMentionName).userId)

        // Multi-photo album + caption
        val albumSend = SendMessage(
            contents = listOf(
                ParaboxPlainText("Trip photos"),
                ParaboxImage(100, 100, "a.jpg", ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/a.jpg")),
                ParaboxImage(100, 100, "b.jpg", ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/b.jpg")),
            ),
            chat = chat,
            timestamp = 1700000040L,
            uuid = "out-4",
        )
        val albumOutbound = TdMessageMapper.mapSendMessageToOutbound(albumSend, "acct-1", dummyMediaBridge)
        assertTrue(albumOutbound.canSendAsAlbum)
        assertEquals(2, albumOutbound.contents.size)
        assertEquals("Trip photos", (albumOutbound.contents[0] as TdApi.InputMessagePhoto).caption.text)

        // Audio / Video / File with captions
        val videoSend = SendMessage(
            contents = listOf(
                ParaboxPlainText("clip"),
                ParaboxVideo(12L, "clip.mp4", 2048L, ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/clip.mp4")),
            ),
            chat = chat,
            timestamp = 1700000050L,
            uuid = "out-5",
        )
        val videoOutbound = TdMessageMapper.mapSendMessageToOutbound(videoSend, "acct-1", dummyMediaBridge)
        assertEquals(1, videoOutbound.contents.size)
        val videoContent = videoOutbound.contents[0] as TdApi.InputMessageVideo
        assertEquals("clip", videoContent.caption.text)
        assertEquals(12, videoContent.duration)

        val audioSend = SendMessage(
            contents = listOf(
                ParaboxAudio(5L, "voice.m4a", 1024L, ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/voice.m4a")),
            ),
            chat = chat,
            timestamp = 1700000060L,
            uuid = "out-6",
        )
        val audioOutbound = TdMessageMapper.mapSendMessageToOutbound(audioSend, "acct-1", dummyMediaBridge)
        assertEquals(1, audioOutbound.contents.size)
        assertTrue(audioOutbound.contents[0] is TdApi.InputMessageVoiceNote)

        val fileSend = SendMessage(
            contents = listOf(
                ParaboxFile("report.pdf", "pdf", 4096L, 1700000070L, ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/report.pdf")),
            ),
            chat = chat,
            timestamp = 1700000070L,
            uuid = "out-7",
        )
        val fileOutbound = TdMessageMapper.mapSendMessageToOutbound(fileSend, "acct-1", dummyMediaBridge)
        assertEquals(1, fileOutbound.contents.size)
        assertTrue(fileOutbound.contents[0] is TdApi.InputMessageDocument)

        // Unreadable media reports errorReason
        val failingBridge = object : TdMediaBridge {
            override suspend fun downloadAndShareFile(
                client: TdClientApi,
                tdFile: TdApi.File,
                hostPackageName: String,
                maxSizeBytes: Long,
                timeoutMs: Long,
            ) = ParaboxResourceInfo.ParaboxEmptyInfo

            override suspend fun stageResourceForUpload(
                accountId: String,
                resourceInfo: ParaboxResourceInfo,
                suggestedName: String?,
            ): File? = null
        }
        val failedOutbound = TdMessageMapper.mapSendMessageToOutbound(fileSend, "acct-1", failingBridge)
        assertTrue(failedOutbound.contents.isEmpty())
        assertEquals(1, failedOutbound.failedMediaCount)
        assertFalse(failedOutbound.errorReason.isNullOrBlank())
    }
}
