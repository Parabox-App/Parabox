package com.ojhdtapp.parabox.data.remote.dto

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.LatestMessage
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString

fun ReceiveMessageDto.toContactEntity(context: Context): ContactEntity {
    return ContactEntity(
        profile = subjectProfile.toProfile(context),
        latestMessage = LatestMessage(
            sender = profile.name,
            content = contents.getContentString(),
            timestamp = timestamp,
            unreadMessagesNum = 0,
        ),
        contactId = pluginConnection.objectId,
        senderId = pluginConnection.objectId,
        isHidden = false,
        isPinned = false,
        isArchived = false,
        enableNotifications = true,
        tags = emptyList()
    )
}

fun ReceiveMessageDto.toMessageEntity(context: Context): MessageEntity {
    return MessageEntity(
        contents = contents.toMessageContentList(context),
        contentString = contents.getContentString(),
        profile = profile.toProfile(context),
        timestamp = timestamp,
        messageId = messageId ?: 0,
        sentByMe = false,
        verified = true
    )
}

fun ReceiveMessageDto.toMessage(context: Context, messageIdInDatabase: Long = 0): Message {
    return Message(
        contents = contents.toMessageContentList(context),
        profile = profile.toProfile(context),
        timestamp = timestamp,
        messageId = messageId ?: messageIdInDatabase,
        sentByMe = false,
        verified = true
    )
}

fun ReceiveMessageDto.toContact(context: Context): Contact {
    return Contact(
        profile = subjectProfile.toProfile(context),
        latestMessage = LatestMessage(
            sender = profile.name,
            content = contents.getContentString(),
            timestamp = timestamp,
            unreadMessagesNum = 0,
        ),
        contactId = pluginConnection.objectId,
        senderId = pluginConnection.objectId,
        isHidden = false,
        isPinned = false,
        isArchived = false,
        shouldBackup = false,
        enableNotifications = true,
        tags = emptyList()
    )
}

//fun SendMessageDto.toMessageEntity(): MessageEntity {
//    return MessageEntity(
//        contents = contents.toMessageContentList(),
//        profile = Profile("", null, null),
//        timestamp = timestamp,
//        sentByMe = true,
//        verified = false
//    )
//}
//
//fun SendMessageDto.toContactEntity(senderName: String): ContactEntity {
//    return ContactEntity(
//        profile = Profile(pluginConnection.id.toString(), null, null),
//        latestMessage = LatestMessage(
//            sender = senderName,
//            content = contents.getContentString(),
//            timestamp = timestamp,
//            unreadMessagesNum = 0,
//        ),
//        contactId = pluginConnection.objectId,
//        senderId = pluginConnection.objectId,
//        isHidden = false,
//        isPinned = false,
//        isArchived = false,
//        enableNotifications = true,
//        tags = emptyList()
//    )
//}

fun com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile.toProfile(context: Context): Profile {
    return Profile(this.name, this.avatar, this.avatarUri?.let {
        FileUtil.getUriByCopyingFileToPath(
            context,
            context.getExternalFilesDir("chat")!!,
            "Image_${this.name}.png",
            it
        )?.toString()
    }, this.id)
}

fun List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>.toMessageContentList(
    context: Context
): List<MessageContent> {
    return this.map {
        it.toMessageContent(context = context)
    }
}

fun com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent.toMessageContent(
    context: Context
): MessageContent {
    return when (this) {
        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText -> PlainText(this.text)
        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image -> Image(
            url,
            width,
            height,
            fileName ?: "Image_${System.currentTimeMillis().toDateAndTimeString()}.png",
            uri?.toString()
        )

        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.At -> com.ojhdtapp.parabox.domain.model.message_content.At(
            target,
            name
        )

        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.AtAll -> com.ojhdtapp.parabox.domain.model.message_content.AtAll
        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio -> {
//            val path = context.getExternalFilesDir("chat")!!
//            val copiedPath = uri?.let {
//                FileUtil.copyFileToPath(
//                    context,
//                    path,
//                    fileName ?: "Audio_${System.currentTimeMillis().toDateAndTimeString()}.mp3",
//                    it
//                )
//            }
//            val copiedUri = copiedPath?.let { FileUtil.getUriOfFile(context, it) }
            Audio(
                url,
                length,
                fileName ?: "Audio_${System.currentTimeMillis().toDateAndTimeString()}.mp3",
                fileSize,
                uri?.toString()
            )
        }

        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.QuoteReply -> {
            quoteMessageContent
            com.ojhdtapp.parabox.domain.model.message_content.QuoteReply(
                quoteMessageSenderName,
                quoteMessageTimestamp,
                quoteMessageId,
                quoteMessageContent?.toMessageContentList(context)
            )
        }

        is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File -> com.ojhdtapp.parabox.domain.model.message_content.File(
            url,
            name,
            extension,
            size,
            lastModifiedTime,
            expiryTime,
            uri?.toString()
        )

        else -> PlainText(this.getContentString())
    }
}

fun com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection.toPluginConnection(): PluginConnection {
    return PluginConnection(this.connectionType, this.objectId, this.id)
}

suspend fun List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>.saveLocalResourcesToCloud(
    context: Context
): List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent> {
    return this.map {
        when (it) {
            is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image -> {
                val cloudResourceInfo = it.uri?.let {
                    FileUtil.getCloudResourceInfoWithSelectedCloudStorage(
                        context,
                        FileUtil.getFileName(
                            context,
                            it
                        ) ?: "Image_${System.currentTimeMillis().toDateAndTimeString()}.jpg",
                        it
                    )
                }
                if (cloudResourceInfo != null) {
                    it.copy(
                        cloudType = cloudResourceInfo.cloudType,
                        url = cloudResourceInfo.url,
                        cloudId = cloudResourceInfo.cloudId
                    )
                } else {
                    com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(text = "[图片]")
                }
            }

            is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio -> {
                val cloudResourceInfo = it.uri?.let {
                    FileUtil.getCloudResourceInfoWithSelectedCloudStorage(
                        context,
                        FileUtil.getFileName(
                            context,
                            it
                        ) ?: "Audio_${System.currentTimeMillis().toDateAndTimeString()}.mp3",
                        it
                    )
                }
                if (cloudResourceInfo != null) {
                    it.copy(
                        cloudType = cloudResourceInfo.cloudType,
                        url = cloudResourceInfo.url,
                        cloudId = cloudResourceInfo.cloudId
                    )
                } else {
                    com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(text = "[语音]")
                }
            }

            is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.QuoteReply -> {
                it.copy(
                    quoteMessageContent = it.quoteMessageContent?.saveLocalResourcesToCloud(
                        context
                    )
                )
            }

            is com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File -> {
                val cloudResourceInfo = it.uri?.let { uri ->
                    FileUtil.getCloudResourceInfoWithSelectedCloudStorage(
                        context,
                        it.name,
                        uri
                    )
                }
                if (cloudResourceInfo != null) {
                    it.copy(
                        cloudType = cloudResourceInfo.cloudType,
                        url = cloudResourceInfo.url,
                        cloudId = cloudResourceInfo.cloudId
                    )
                } else {
                    com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(text = "[文件]")
                }
            }

            else -> it
        }
    }
}