package com.ojhdt.parabox.extension.tdlib.mapper

import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import org.drinkless.tdlib.TdApi

data class OutboundTdPayload(
    val contents: List<TdApi.InputMessageContent>,
    val replyToMessageId: Long? = null,
)

object TdMessageMapper {

    fun formatMessageUuid(chatId: Long, messageId: Long): String = "$chatId:$messageId"

    fun parseMessageUuid(uuid: String): Pair<Long, Long>? {
        val parts = uuid.split(":")
        if (parts.size != 2) return null
        val chatId = parts[0].toLongOrNull() ?: return null
        val msgId = parts[1].toLongOrNull() ?: return null
        return chatId to msgId
    }

    fun formatUserDisplayName(user: TdApi.User): String {
        val full = buildString {
            append(user.firstName.orEmpty().trim())
            val last = user.lastName.orEmpty().trim()
            if (last.isNotEmpty()) {
                if (isNotEmpty()) append(' ')
                append(last)
            }
        }
        return full.ifBlank { user.phoneNumber?.ifBlank { null } ?: "User ${user.id}" }
    }

    fun mapChatType(type: TdApi.ChatType?): Int = when (type) {
        is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> ParaboxChat.TYPE_PRIVATE
        is TdApi.ChatTypeBasicGroup, is TdApi.ChatTypeSupergroup -> ParaboxChat.TYPE_GROUP
        else -> ParaboxChat.TYPE_OTHER
    }

    suspend fun mapMessageContentToElements(
        message: TdApi.Message,
        client: TdClientApi,
        mediaBridge: TdMediaBridge,
        hostPackageName: String,
        autoDownloadMedia: Boolean,
        maxDownloadBytes: Long,
    ): List<ParaboxMessageElement> {
        val elements = mutableListOf<ParaboxMessageElement>()

        // Preserve quote-reply metadata if present
        val reply = message.replyTo
        if (reply is TdApi.MessageReplyToMessage) {
            val quoteText = reply.quote?.text?.text?.takeIf { it.isNotBlank() }
            val quoteMessages = if (quoteText != null) {
                listOf(ParaboxPlainText(quoteText))
            } else {
                emptyList()
            }
            elements.add(
                ParaboxQuoteReply(
                    sender = null,
                    timestamp = reply.originSendDate.takeIf { it > 0 }?.toLong()?.times(1000L),
                    id = formatMessageUuid(reply.chatId, reply.messageId),
                    messages = quoteMessages,
                )
            )
        }

        when (val content = message.content) {
            is TdApi.MessageText -> {
                val text = content.text?.text.orEmpty()
                if (text.isNotEmpty()) {
                    elements.add(ParaboxPlainText(text))
                }
            }

            is TdApi.MessagePhoto -> {
                val bestSize = content.photo?.sizes?.maxByOrNull { it.width * it.height }
                val resInfo = if (autoDownloadMedia && bestSize?.photo != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = bestSize.photo,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxImage(
                        width = bestSize?.width ?: 0,
                        height = bestSize?.height ?: 0,
                        fileName = bestSize?.photo?.id?.let { "tg_photo_$it.jpg" },
                        resourceInfo = resInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) {
                    elements.add(ParaboxPlainText(caption))
                }
            }

            is TdApi.MessageVoiceNote -> {
                val voice = content.voiceNote
                val resInfo = if (autoDownloadMedia && voice?.voice != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = voice.voice,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxAudio(
                        length = (voice?.duration ?: 0).toLong(),
                        fileName = voice?.voice?.id?.let { "tg_voice_$it.ogg" },
                        fileSize = voice?.voice?.expectedSize ?: 0L,
                        resourceInfo = resInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageAudio -> {
                val audio = content.audio
                val resInfo = if (autoDownloadMedia && audio?.audio != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = audio.audio,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxAudio(
                        length = (audio?.duration ?: 0).toLong(),
                        fileName = audio?.fileName?.ifBlank { null },
                        fileSize = audio?.audio?.expectedSize ?: 0L,
                        resourceInfo = resInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageVideo -> {
                val video = content.video
                val resInfo = if (autoDownloadMedia && video?.video != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = video.video,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxVideo(
                        length = (video?.duration ?: 0).toLong(),
                        fileName = video?.fileName?.ifBlank { null },
                        fileSize = video?.video?.expectedSize ?: 0L,
                        resourceInfo = resInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageVideoNote -> {
                val note = content.videoNote
                val resInfo = if (autoDownloadMedia && note?.video != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = note.video,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxVideo(
                        length = (note?.duration ?: 0).toLong(),
                        fileName = note?.video?.id?.let { "tg_videonote_$it.mp4" },
                        fileSize = note?.video?.expectedSize ?: 0L,
                        resourceInfo = resInfo,
                    )
                )
            }

            is TdApi.MessageDocument -> {
                val doc = content.document
                val fileName = doc?.fileName?.ifBlank { "document" } ?: "document"
                val ext = fileName.substringAfterLast('.', "")
                val resInfo = if (autoDownloadMedia && doc?.document != null) {
                    mediaBridge.downloadAndShareFile(
                        client = client,
                        tdFile = doc.document,
                        hostPackageName = hostPackageName,
                        maxSizeBytes = maxDownloadBytes,
                    )
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                elements.add(
                    ParaboxFile(
                        name = fileName,
                        extension = ext,
                        size = doc?.document?.expectedSize ?: 0L,
                        lastModifiedTime = message.date.toLong() * 1000L,
                        resourceInfo = resInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageLocation -> {
                val loc = content.location
                if (loc != null) {
                    elements.add(
                        ParaboxLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            name = null,
                            description = null,
                        )
                    )
                } else {
                    elements.add(ParaboxUnsupported)
                }
            }

            is TdApi.MessageSticker -> {
                val emoji = content.sticker?.emoji.orEmpty()
                elements.add(ParaboxPlainText("[贴纸${if (emoji.isNotEmpty()) " $emoji" else ""}]"))
            }

            is TdApi.MessageAnimation -> {
                val caption = content.caption?.text.orEmpty()
                val label = if (caption.isNotBlank()) "[动图] $caption" else "[动图]"
                elements.add(ParaboxPlainText(label))
            }

            else -> {
                elements.add(ParaboxUnsupported)
            }
        }

        if (elements.isEmpty()) {
            elements.add(ParaboxUnsupported)
        }
        return elements
    }

    suspend fun mapSendMessageToOutbound(
        sendMessage: SendMessage,
        accountId: String,
        mediaBridge: TdMediaBridge,
    ): OutboundTdPayload {
        val textParts = mutableListOf<String>()
        val mediaContents = mutableListOf<TdApi.InputMessageContent>()
        var replyToMessageId: Long? = null

        for (element in sendMessage.contents) {
            when (element) {
                is ParaboxPlainText -> {
                    if (element.text.isNotEmpty()) textParts.add(element.text)
                }
                is ParaboxQuoteReply -> {
                    val parsed = element.id?.let { parseMessageUuid(it) }
                    if (parsed != null) {
                        replyToMessageId = parsed.second
                    }
                }
                is ParaboxImage -> {
                    val file = stageResource(accountId, element.resourceInfo, element.fileName, mediaBridge)
                    if (file != null) {
                        mediaContents.add(
                            TdApi.InputMessagePhoto(
                                TdApi.InputFileLocal(file.absolutePath),
                                null,
                                intArrayOf(),
                                element.width,
                                element.height,
                                null,
                                false,
                                null,
                                false,
                            )
                        )
                    }
                }
                is ParaboxAudio -> {
                    val file = stageResource(accountId, element.resourceInfo, element.fileName, mediaBridge)
                    if (file != null) {
                        mediaContents.add(
                            TdApi.InputMessageVoiceNote(
                                TdApi.InputFileLocal(file.absolutePath),
                                element.length.toInt(),
                                byteArrayOf(),
                                null,
                                null,
                            )
                        )
                    }
                }
                is ParaboxVideo -> {
                    val file = stageResource(accountId, element.resourceInfo, element.fileName, mediaBridge)
                    if (file != null) {
                        mediaContents.add(
                            TdApi.InputMessageVideo(
                                TdApi.InputFileLocal(file.absolutePath),
                                null,
                                intArrayOf(),
                                element.length.toInt(),
                                0,
                                0,
                                true,
                                null,
                                false,
                                null,
                                false,
                            )
                        )
                    }
                }
                is ParaboxFile -> {
                    val file = stageResource(accountId, element.resourceInfo, element.name, mediaBridge)
                    if (file != null) {
                        mediaContents.add(
                            TdApi.InputMessageDocument(
                                TdApi.InputFileLocal(file.absolutePath),
                                null,
                                false,
                                null,
                            )
                        )
                    }
                }
                is ParaboxLocation -> {
                    mediaContents.add(
                        TdApi.InputMessageLocation(
                            TdApi.Location(element.latitude, element.longitude, 0.0),
                            0,
                            0,
                            0,
                        )
                    )
                }
                else -> {
                    val fallback = element.contentToString()
                    if (fallback.isNotBlank()) textParts.add(fallback)
                }
            }
        }

        val combinedText = textParts.joinToString("\n").trim()
        val resultContents = mutableListOf<TdApi.InputMessageContent>()
        if (mediaContents.size == 1 && combinedText.isNotEmpty() && mediaContents[0] is TdApi.InputMessagePhoto) {
            val photo = mediaContents[0] as TdApi.InputMessagePhoto
            photo.caption = TdApi.FormattedText(combinedText, emptyArray())
            resultContents.add(photo)
        } else {
            if (combinedText.isNotEmpty()) {
                resultContents.add(
                    TdApi.InputMessageText(
                        TdApi.FormattedText(combinedText, emptyArray()),
                        null,
                        true,
                    )
                )
            }
            resultContents.addAll(mediaContents)
        }

        return OutboundTdPayload(
            contents = resultContents,
            replyToMessageId = replyToMessageId,
        )
    }

    private suspend fun stageResource(
        accountId: String,
        resourceInfo: ParaboxResourceInfo,
        fileName: String?,
        mediaBridge: TdMediaBridge,
    ): java.io.File? {
        return mediaBridge.stageResourceForUpload(accountId, resourceInfo, fileName)
    }

    fun buildContact(uid: String, name: String?, avatar: ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo) =
        ParaboxContact(
            basicInfo = ParaboxBasicInfo(name = name, avatar = avatar),
            uid = uid,
        )
}
