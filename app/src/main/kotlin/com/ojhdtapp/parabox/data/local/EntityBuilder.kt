package com.ojhdtapp.parabox.data.local

import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

fun buildContactEntity(msg: ReceiveMessage, ext: ConnectionInfo): ContactEntity {
    return ContactEntity(
        name = msg.sender.basicInfo.name,
        avatar = msg.sender.basicInfo.avatar,
        pkg = ext.pkg,
        uid = msg.sender.uid,
        connectionId = ext.connectionId
    )
}

fun buildChatEntity(msg: ReceiveMessage, ext: ConnectionInfo): ChatEntity {
    return ChatEntity(
        name = msg.chat.basicInfo.name,
        avatar = msg.chat.basicInfo.avatar,
        latestMessageId = null,
        isHidden = false,
        isPinned = false,
        isArchived = false,
        isNotificationEnabled = true,
        tags = emptyList(),
        subChatIds = emptyList(),
        type = msg.chat.type,
        pkg = ext.pkg,
        uid = msg.chat.uid,
        extensionId = ext.connectionId
    )
}

private fun computeContentTypesMask(contents: List<ParaboxMessageElement>): Int {
    val typeList = contents.map { it.getType() }
    val maxType = typeList.maxOrNull() ?: return 0
    return buildString {
        (0..maxType).forEach {
            if (it in typeList) {
                insert(0, 1)
            } else {
                insert(0, 0)
            }
        }
    }.ifBlank { "0" }.toInt(2)
}

fun buildMessageEntity(msg: ReceiveMessage, ext: ConnectionInfo, senderId: Long, chatId: Long): MessageEntity {
    return MessageEntity(
        contents = msg.contents,
        contentTypes = computeContentTypesMask(msg.contents),
        contentString = msg.contents.joinToString { it.contentToString() },
        senderId = senderId,
        chatId = chatId,
        timestamp = msg.timestamp,
        sentByMe = false,
        verified = true,
        pkg = ext.pkg,
        uid = msg.uuid,
        extensionId = ext.connectionId
    )
}

fun buildOutgoingMessageEntity(
    msg: SendMessage,
    pkg: String,
    extensionId: Long,
    senderId: Long,
    chatId: Long,
): MessageEntity {
    return MessageEntity(
        contents = msg.contents,
        contentTypes = computeContentTypesMask(msg.contents),
        contentString = msg.contents.joinToString(" ") { it.contentToString() },
        senderId = senderId,
        chatId = chatId,
        timestamp = msg.timestamp,
        sentByMe = true,
        verified = false,
        pkg = pkg,
        uid = msg.uuid,
        extensionId = extensionId,
    )
}
