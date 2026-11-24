package com.ojhdt.parabox.extension.tdlib.mapper

import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAnnotatedText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAtAll
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import org.drinkless.tdlib.TdApi
import java.io.File

data class OutboundTdPayload(
    val contents: List<TdApi.InputMessageContent>,
    val replyToMessageId: Long? = null,
    val stagedFiles: List<File> = emptyList(),
    val failedMediaCount: Int = 0,
    val errorReason: String? = null,
) {
    val canSendAsAlbum: Boolean
        get() = contents.size in 2..10 && contents.all {
            it is TdApi.InputMessagePhoto || it is TdApi.InputMessageVideo
        }
}

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

    fun mapContentToQuotePreviewElements(
        content: TdApi.MessageContent?,
        messageDateSeconds: Int = 0,
    ): List<ParaboxMessageElement> {
        if (content == null) return emptyList()
        val elements = mutableListOf<ParaboxMessageElement>()
        when (content) {
            is TdApi.MessageText -> {
                val text = content.text?.text.orEmpty()
                if (text.isNotEmpty()) {
                    elements.add(ParaboxPlainText(text))
                }
            }

            is TdApi.MessagePhoto -> {
                val bestSize = content.photo?.sizes?.maxByOrNull { it.width * it.height }
                elements.add(
                    ParaboxImage(
                        width = bestSize?.width ?: 0,
                        height = bestSize?.height ?: 0,
                        fileName = bestSize?.photo?.id?.let { "tg_photo_$it.jpg" },
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) {
                    elements.add(ParaboxPlainText(caption))
                }
            }

            is TdApi.MessageVoiceNote -> {
                val voice = content.voiceNote
                elements.add(
                    ParaboxAudio(
                        length = (voice?.duration ?: 0).toLong(),
                        fileName = voice?.voice?.id?.let { "tg_voice_$it.ogg" },
                        fileSize = voice?.voice?.expectedSize ?: 0L,
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageAudio -> {
                val audio = content.audio
                elements.add(
                    ParaboxAudio(
                        length = (audio?.duration ?: 0).toLong(),
                        fileName = audio?.fileName?.ifBlank { null },
                        fileSize = audio?.audio?.expectedSize ?: 0L,
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageVideo -> {
                val video = content.video
                elements.add(
                    ParaboxVideo(
                        length = (video?.duration ?: 0).toLong(),
                        fileName = video?.fileName?.ifBlank { null },
                        fileSize = video?.video?.expectedSize ?: 0L,
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
                    )
                )
                val caption = content.caption?.text.orEmpty()
                if (caption.isNotBlank()) elements.add(ParaboxPlainText(caption))
            }

            is TdApi.MessageVideoNote -> {
                val note = content.videoNote
                elements.add(
                    ParaboxVideo(
                        length = (note?.duration ?: 0).toLong(),
                        fileName = note?.video?.id?.let { "tg_videonote_$it.mp4" },
                        fileSize = note?.video?.expectedSize ?: 0L,
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
                    )
                )
            }

            is TdApi.MessageDocument -> {
                val doc = content.document
                val fileName = doc?.fileName?.ifBlank { "document" } ?: "document"
                val ext = fileName.substringAfterLast('.', "")
                elements.add(
                    ParaboxFile(
                        name = fileName,
                        extension = ext,
                        size = doc?.document?.expectedSize ?: 0L,
                        lastModifiedTime = messageDateSeconds.takeIf { it > 0 }?.toLong()?.times(1000L) ?: 0L,
                        resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
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
        return elements
    }

    suspend fun mapMessageContentToElements(
        message: TdApi.Message,
        client: TdClientApi,
        mediaBridge: TdMediaBridge,
        hostPackageName: String,
        autoDownloadMedia: Boolean,
        maxDownloadBytes: Long,
        resolvedQuoteReply: ParaboxQuoteReply? = null,
    ): List<ParaboxMessageElement> {
        val elements = mutableListOf<ParaboxMessageElement>()

        // Preserve quote-reply metadata if present
        if (resolvedQuoteReply != null) {
            elements.add(resolvedQuoteReply)
        } else {
            val reply = message.replyTo
            if (reply is TdApi.MessageReplyToMessage) {
                val quoteText = reply.quote?.text?.text?.takeIf { it.isNotBlank() }
                val quoteMessages = if (quoteText != null) {
                    listOf(ParaboxPlainText(quoteText))
                } else {
                    mapContentToQuotePreviewElements(reply.content, reply.originSendDate)
                }
                val quoteId = if (reply.chatId != 0L && reply.messageId != 0L) {
                    formatMessageUuid(reply.chatId, reply.messageId)
                } else {
                    null
                }
                elements.add(
                    ParaboxQuoteReply(
                        sender = null,
                        timestamp = reply.originSendDate.takeIf { it > 0 }?.toLong()?.times(1000L),
                        id = quoteId,
                        messages = quoteMessages,
                    )
                )
            }
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

    private const val MAX_TELEGRAM_PHOTO_BYTES = 10L * 1024L * 1024L
    private val VOICE_NOTE_EXTENSIONS = setOf("ogg", "oga", "opus", "m4a", "mp3", "aac")

    suspend fun mapSendMessageToOutbound(
        sendMessage: SendMessage,
        accountId: String,
        mediaBridge: TdMediaBridge,
    ): OutboundTdPayload {
        val textBuilder = StringBuilder()
        val textEntities = mutableListOf<TdApi.TextEntity>()
        val mediaContents = mutableListOf<TdApi.InputMessageContent>()
        val stagedFiles = mutableListOf<File>()
        var replyToMessageId: Long? = null
        var failedMediaCount = 0

        fun appendPlainSegment(segment: String) {
            if (segment.isEmpty()) return
            if (textBuilder.isNotEmpty() && !textBuilder.endsWith('\n') && !textBuilder.endsWith(' ')) {
                textBuilder.append(' ')
            }
            textBuilder.append(segment)
        }

        fun appendTextElement(textElement: ParaboxText) {
            when (textElement) {
                is ParaboxPlainText -> {
                    if (textElement.text.isNotEmpty()) {
                        textBuilder.append(textElement.text)
                    }
                }
                is ParaboxAt -> {
                    val label = "@${textElement.target.basicInfo.name?.ifBlank { null } ?: textElement.target.uid}"
                    if (textBuilder.isNotEmpty() && !textBuilder.endsWith(' ') && !textBuilder.endsWith('\n')) {
                        textBuilder.append(' ')
                    }
                    val offset = textBuilder.length
                    textBuilder.append(label)
                    val userId = textElement.target.uid.toLongOrNull()
                    if (userId != null && userId > 0L) {
                        textEntities.add(
                            TdApi.TextEntity(
                                offset,
                                label.length,
                                TdApi.TextEntityTypeMentionName(userId),
                            )
                        )
                    }
                    textBuilder.append(' ')
                }
                is ParaboxAtAll -> {
                    appendPlainSegment("@all")
                }
            }
        }

        for (element in sendMessage.contents) {
            when (element) {
                is ParaboxText -> appendTextElement(element)
                is ParaboxAnnotatedText -> {
                    element.list.forEach { appendTextElement(it) }
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
                        stagedFiles.add(file)
                        val meta = if (element.width <= 0 || element.height <= 0) {
                            mediaBridge.probeImageMetadata(file)
                        } else {
                            MediaMetadata(element.width, element.height, 0, file.length())
                        }
                        if (meta.sizeBytes > MAX_TELEGRAM_PHOTO_BYTES) {
                            mediaContents.add(
                                TdApi.InputMessageDocument(
                                    TdApi.InputFileLocal(file.absolutePath),
                                    null,
                                    false,
                                    null,
                                )
                            )
                        } else {
                            mediaContents.add(
                                TdApi.InputMessagePhoto(
                                    TdApi.InputFileLocal(file.absolutePath),
                                    null,
                                    intArrayOf(),
                                    meta.width,
                                    meta.height,
                                    null,
                                    false,
                                    null,
                                    false,
                                )
                            )
                        }
                    } else {
                        failedMediaCount++
                    }
                }
                is ParaboxAudio -> {
                    val file = stageResource(accountId, element.resourceInfo, element.fileName ?: "voice.m4a", mediaBridge)
                    if (file != null) {
                        stagedFiles.add(file)
                        val durationSec = element.length.toInt().takeIf { it > 0 }
                            ?: mediaBridge.probeAudioDurationSeconds(file)
                        val ext = file.extension.lowercase()
                        if (ext.isEmpty() || ext in VOICE_NOTE_EXTENSIONS) {
                            mediaContents.add(
                                TdApi.InputMessageVoiceNote(
                                    TdApi.InputFileLocal(file.absolutePath),
                                    durationSec,
                                    byteArrayOf(),
                                    null,
                                    null,
                                )
                            )
                        } else {
                            mediaContents.add(
                                TdApi.InputMessageAudio(
                                    TdApi.InputFileLocal(file.absolutePath),
                                    null,
                                    durationSec,
                                    file.nameWithoutExtension,
                                    "",
                                    null,
                                )
                            )
                        }
                    } else {
                        failedMediaCount++
                    }
                }
                is ParaboxVideo -> {
                    val file = stageResource(accountId, element.resourceInfo, element.fileName ?: "video.mp4", mediaBridge)
                    if (file != null) {
                        stagedFiles.add(file)
                        val meta = mediaBridge.probeVideoMetadata(file)
                        val durationSec = element.length.toInt().takeIf { it > 0 } ?: meta.durationSeconds
                        mediaContents.add(
                            TdApi.InputMessageVideo(
                                TdApi.InputFileLocal(file.absolutePath),
                                null,
                                intArrayOf(),
                                durationSec,
                                meta.width,
                                meta.height,
                                true,
                                null,
                                false,
                                null,
                                false,
                            )
                        )
                    } else {
                        failedMediaCount++
                    }
                }
                is ParaboxFile -> {
                    val file = stageResource(accountId, element.resourceInfo, element.name, mediaBridge)
                    if (file != null) {
                        stagedFiles.add(file)
                        mediaContents.add(
                            TdApi.InputMessageDocument(
                                TdApi.InputFileLocal(file.absolutePath),
                                null,
                                false,
                                null,
                            )
                        )
                    } else {
                        failedMediaCount++
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
                    if (fallback.isNotBlank()) appendPlainSegment(fallback)
                }
            }
        }

        val rawText = textBuilder.toString()
        val trimmedStart = rawText.length - rawText.trimStart().length
        val finalText = rawText.trim()
        val adjustedEntities = if (finalText.isNotEmpty() && textEntities.isNotEmpty()) {
            textEntities.mapNotNull { entity ->
                val newOffset = entity.offset - trimmedStart
                if (newOffset >= 0 && newOffset + entity.length <= finalText.length) {
                    TdApi.TextEntity(newOffset, entity.length, entity.type)
                } else {
                    null
                }
            }.toTypedArray()
        } else {
            emptyArray()
        }
        val formattedText = if (finalText.isNotEmpty()) {
            TdApi.FormattedText(finalText, adjustedEntities)
        } else {
            null
        }

        val resultContents = mutableListOf<TdApi.InputMessageContent>()
        val isAlbum = mediaContents.size in 2..10 && mediaContents.all {
            it is TdApi.InputMessagePhoto || it is TdApi.InputMessageVideo
        }
        if (formattedText != null && mediaContents.size == 1 && attachCaption(mediaContents[0], formattedText)) {
            resultContents.add(mediaContents[0])
        } else if (formattedText != null && isAlbum && attachCaption(mediaContents[0], formattedText)) {
            resultContents.addAll(mediaContents)
        } else {
            if (formattedText != null) {
                resultContents.add(
                    TdApi.InputMessageText(
                        formattedText,
                        null,
                        true,
                    )
                )
            }
            resultContents.addAll(mediaContents)
        }

        val errorReason = when {
            resultContents.isEmpty() && failedMediaCount > 0 -> "媒体文件不可读或已被删除"
            resultContents.isEmpty() -> "消息内容为空"
            else -> null
        }

        return OutboundTdPayload(
            contents = resultContents,
            replyToMessageId = replyToMessageId,
            stagedFiles = stagedFiles,
            failedMediaCount = failedMediaCount,
            errorReason = errorReason,
        )
    }

    private fun attachCaption(content: TdApi.InputMessageContent, caption: TdApi.FormattedText): Boolean {
        return when (content) {
            is TdApi.InputMessagePhoto -> {
                content.caption = caption
                true
            }
            is TdApi.InputMessageVideo -> {
                content.caption = caption
                true
            }
            is TdApi.InputMessageDocument -> {
                content.caption = caption
                true
            }
            is TdApi.InputMessageAudio -> {
                content.caption = caption
                true
            }
            is TdApi.InputMessageVoiceNote -> {
                content.caption = caption
                true
            }
            else -> false
        }
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
