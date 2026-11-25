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

    @Test
    fun quotePreviewElementsAndUnknownChatReplyId() = runBlocking {
        // Photo + caption preview does not require downloading media
        val photoContent = TdApi.MessagePhoto(
            TdApi.Photo().apply {
                sizes = arrayOf(
                    TdApi.PhotoSize().apply {
                        width = 640
                        height = 480
                        photo = TdApi.File().apply { id = 77 }
                    }
                )
            },
            TdApi.FormattedText("Sunset", emptyArray()),
            false,
            false,
            false,
        )
        val photoPreview = TdMessageMapper.mapContentToQuotePreviewElements(photoContent)
        assertEquals(2, photoPreview.size)
        val img = photoPreview[0] as ParaboxImage
        assertEquals(ParaboxResourceInfo.ParaboxEmptyInfo, img.resourceInfo)
        assertEquals("Sunset", (photoPreview[1] as ParaboxPlainText).text)

        // Document preview
        val docContent = TdApi.MessageDocument(
            TdApi.Document().apply {
                fileName = "spec.pdf"
                document = TdApi.File().apply { expectedSize = 1024L }
            },
            TdApi.FormattedText("", emptyArray()),
        )
        val docPreview = TdMessageMapper.mapContentToQuotePreviewElements(docContent, 1700000000)
        assertEquals(1, docPreview.size)
        val fileElem = docPreview[0] as ParaboxFile
        assertEquals("spec.pdf", fileElem.name)
        assertEquals(ParaboxResourceInfo.ParaboxEmptyInfo, fileElem.resourceInfo)

        // Unknown chat (chatId == 0) should yield null quote id and preserve fallback content
        val unknownReplyMsg = TdApi.Message().apply {
            id = 888L
            chatId = -10010L
            date = 1700000100
            replyTo = TdApi.MessageReplyToMessage(
                0L,
                0L,
                null,
                TdApi.MessageOriginHiddenUser("Hidden Friend"),
                1700000050,
                TdApi.MessageText(TdApi.FormattedText("External quote", emptyArray()), null, null),
            )
            content = TdApi.MessageText(TdApi.FormattedText("Reply text", emptyArray()), null, null)
        }
        val elements = TdMessageMapper.mapMessageContentToElements(
            message = unknownReplyMsg,
            client = dummyClient,
            mediaBridge = dummyMediaBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
        )
        val quote = elements[0] as ParaboxQuoteReply
        assertEquals(null, quote.id)
        assertEquals(1700000050L * 1000L, quote.timestamp)
        assertEquals(listOf(ParaboxPlainText("External quote")), quote.messages)
    }

    @Test
    fun inboundPhotoStickerAndAnimationUseLazyUrisWithoutEagerDownload() = runBlocking {
        var downloadCalls = 0
        val countingBridge = object : TdMediaBridge {
            override suspend fun downloadAndShareFile(
                client: TdClientApi,
                tdFile: TdApi.File,
                hostPackageName: String,
                maxSizeBytes: Long,
                timeoutMs: Long,
            ): ParaboxResourceInfo {
                downloadCalls++
                return ParaboxResourceInfo.ParaboxEmptyInfo
            }

            override suspend fun stageResourceForUpload(
                accountId: String,
                resourceInfo: ParaboxResourceInfo,
                suggestedName: String?,
            ): File? = null
        }

        fun makeFile(id: Int, remoteId: String): TdApi.File = TdApi.File().apply {
            this.id = id
            this.remote = TdApi.RemoteFile().apply { this.id = remoteId }
        }

        // 1. Photo -> lazy remote URI, 0 eager downloads
        val photoMsg = TdApi.Message().apply {
            id = 1L
            chatId = -10010L
            content = TdApi.MessagePhoto(
                TdApi.Photo().apply {
                    sizes = arrayOf(
                        TdApi.PhotoSize().apply {
                            width = 800
                            height = 600
                            photo = makeFile(11, "rem-photo-1")
                        }
                    )
                },
                TdApi.FormattedText("Hi", emptyArray()),
                false,
                false,
                false,
            )
        }
        val photoElements = TdMessageMapper.mapMessageContentToElements(
            message = photoMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(2, photoElements.size)
        val photoImg = photoElements[0] as ParaboxImage
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-photo-1"),
            photoImg.resourceInfo,
        )
        assertEquals(ParaboxPlainText("Hi"), photoElements[1])

        // 2. WebP Sticker -> lazy remote URI on sticker file directly
        val webpStickerMsg = TdApi.Message().apply {
            id = 2L
            chatId = -10010L
            content = TdApi.MessageSticker(
                TdApi.Sticker().apply {
                    width = 512
                    height = 512
                    emoji = "🔥"
                    format = TdApi.StickerFormatWebp()
                    sticker = makeFile(21, "rem-webp-1")
                },
                false,
            )
        }
        val webpElements = TdMessageMapper.mapMessageContentToElements(
            message = webpStickerMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(1, webpElements.size)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-webp-1"),
            (webpElements[0] as ParaboxImage).resourceInfo,
        )

        // 3. TGS Sticker -> lazy remote URI directly on .tgs file
        val tgsStickerMsg = TdApi.Message().apply {
            id = 3L
            chatId = -10010L
            content = TdApi.MessageSticker(
                TdApi.Sticker().apply {
                    width = 512
                    height = 512
                    emoji = "✨"
                    format = TdApi.StickerFormatTgs()
                    sticker = makeFile(31, "rem-tgs-1")
                    thumbnail = TdApi.Thumbnail().apply {
                        width = 128
                        height = 128
                        format = TdApi.ThumbnailFormatWebp()
                        file = makeFile(32, "rem-tgs-thumb-1")
                    }
                },
                false,
            )
        }
        val tgsElements = TdMessageMapper.mapMessageContentToElements(
            message = tgsStickerMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(1, tgsElements.size)
        val tgsImage = tgsElements[0] as ParaboxImage
        assertEquals("tg_sticker_31.tgs", tgsImage.fileName)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-tgs-1"),
            tgsImage.resourceInfo,
        )

        // 3b. WebM Video Sticker -> lazy remote URI directly on .webm file
        val webmStickerMsg = TdApi.Message().apply {
            id = 33L
            chatId = -10010L
            content = TdApi.MessageSticker(
                TdApi.Sticker().apply {
                    width = 512
                    height = 512
                    emoji = "🎬"
                    format = TdApi.StickerFormatWebm()
                    sticker = makeFile(35, "rem-webm-1")
                },
                false,
            )
        }
        val webmElements = TdMessageMapper.mapMessageContentToElements(
            message = webmStickerMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(1, webmElements.size)
        val webmImage = webmElements[0] as ParaboxImage
        assertEquals("tg_sticker_35.webm", webmImage.fileName)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-webm-1"),
            webmImage.resourceInfo,
        )

        // 4. GIF Animation -> direct lazy remote URI; MP4 Animation -> direct lazy .mp4 URI
        val gifAnimMsg = TdApi.Message().apply {
            id = 4L
            chatId = -10010L
            content = TdApi.MessageAnimation(
                TdApi.Animation().apply {
                    width = 320
                    height = 240
                    fileName = "party.gif"
                    mimeType = "image/gif"
                    animation = makeFile(41, "rem-gif-1")
                },
                TdApi.FormattedText("", emptyArray()),
                false,
                false,
                false,
            )
        }
        val gifElements = TdMessageMapper.mapMessageContentToElements(
            message = gifAnimMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(1, gifElements.size)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-gif-1"),
            (gifElements[0] as ParaboxImage).resourceInfo,
        )

        val mp4AnimMsg = TdApi.Message().apply {
            id = 5L
            chatId = -10010L
            content = TdApi.MessageAnimation(
                TdApi.Animation().apply {
                    width = 320
                    height = 240
                    fileName = "clip.mp4"
                    mimeType = "video/mp4"
                    animation = makeFile(51, "rem-mp4-1")
                    thumbnail = TdApi.Thumbnail().apply {
                        width = 320
                        height = 240
                        format = TdApi.ThumbnailFormatJpeg()
                        file = makeFile(52, "rem-mp4-thumb-1")
                    }
                },
                TdApi.FormattedText("Loop", emptyArray()),
                false,
                false,
                false,
            )
        }
        val mp4Elements = TdMessageMapper.mapMessageContentToElements(
            message = mp4AnimMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(2, mp4Elements.size)
        val mp4Image = mp4Elements[0] as ParaboxImage
        assertEquals("clip.mp4", mp4Image.fileName)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/rem-mp4-1"),
            mp4Image.resourceInfo,
        )
        assertEquals(ParaboxPlainText("Loop"), mp4Elements[1])

        // Zero eager downloads across all photo/sticker/animation messages
        assertEquals(0, downloadCalls)
    }

    @Test
    fun inboundVoiceAndAudioUseLazyExtensionUriWithoutImmediateDownload() = runBlocking {
        var downloadCount = 0
        val countingBridge = object : TdMediaBridge {
            override suspend fun downloadAndShareFile(
                client: TdClientApi,
                tdFile: TdApi.File,
                hostPackageName: String,
                maxSizeBytes: Long,
                timeoutMs: Long,
            ): ParaboxResourceInfo {
                downloadCount++
                return ParaboxResourceInfo.ParaboxEmptyInfo
            }

            override suspend fun stageResourceForUpload(
                accountId: String,
                resourceInfo: ParaboxResourceInfo,
                suggestedName: String?,
            ): File? = null
        }

        val voiceMsg = TdApi.Message().apply {
            id = 901L
            chatId = -10010L
            date = 1700000200
            content = TdApi.MessageVoiceNote(
                TdApi.VoiceNote().apply {
                    duration = 14
                    mimeType = "audio/ogg"
                    voice = TdApi.File().apply {
                        id = 123
                        expectedSize = 24576L
                        remote = TdApi.RemoteFile().apply { id = "AwACAgIAAxkBAvoice1" }
                    }
                },
                TdApi.FormattedText("", emptyArray()),
                false,
            )
        }
        val voiceElems = TdMessageMapper.mapMessageContentToElements(
            message = voiceMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = true,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(1, voiceElems.size)
        val voiceAudio = voiceElems[0] as ParaboxAudio
        assertEquals(14L, voiceAudio.length)
        assertEquals("tg_voice_123.ogg", voiceAudio.fileName)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/AwACAgIAAxkBAvoice1"),
            voiceAudio.resourceInfo,
        )

        val musicMsg = TdApi.Message().apply {
            id = 902L
            chatId = -10010L
            date = 1700000205
            content = TdApi.MessageAudio(
                TdApi.Audio().apply {
                    duration = 185
                    fileName = "demo_track.mp3"
                    mimeType = "audio/mpeg"
                    audio = TdApi.File().apply {
                        id = 456
                        expectedSize = 3_145_728L
                        remote = TdApi.RemoteFile().apply { id = "CQACAgIAAxkBAmusic2" }
                    }
                },
                TdApi.FormattedText("Listen to this", emptyArray()),
            )
        }
        val musicElems = TdMessageMapper.mapMessageContentToElements(
            message = musicMsg,
            client = dummyClient,
            mediaBridge = countingBridge,
            hostPackageName = "com.ojhdtapp.parabox",
            autoDownloadMedia = false,
            maxDownloadBytes = 10_000_000L,
            sessionId = "conn-7",
        )
        assertEquals(2, musicElems.size)
        val musicAudio = musicElems[0] as ParaboxAudio
        assertEquals(185L, musicAudio.length)
        assertEquals("demo_track.mp3", musicAudio.fileName)
        assertEquals(
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("parabox-ext://conn-7/remote/CQACAgIAAxkBAmusic2"),
            musicAudio.resourceInfo,
        )
        assertEquals(ParaboxPlainText("Listen to this"), musicElems[1])
        assertEquals(0, downloadCount)
    }
}
