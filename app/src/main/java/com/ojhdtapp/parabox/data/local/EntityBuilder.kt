package com.ojhdtapp.parabox.data.local

import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage

fun buildContactEntity(msg: ReceiveMessage, ext: ExtensionInfo): ContactEntity{
    return ContactEntity(
        name = msg.sender.name,
        avatar = msg.sender.avatar,
        pkg = ext.pkg,
        uid = msg.sender.uid,
    )
}

fun buildChatEntity(msg: ReceiveMessage, ext: ExtensionInfo): ChatEntity {
    return ChatEntity(
        name = msg.chat.name,
        avatar = msg.chat.avatar,
        latestMessageId = null,
        isHidden = false,
        isPinned = false,
        isArchived = false,
        isNotificationEnabled = false,
        tags = emptyList(),
        subChatIds = emptyList(),
        pkg = ext.pkg,
        uid = msg.chat.uid
    )
}

fun buildMessageEntity(msg: ReceiveMessage, senderId: Long, chatId: Long): MessageEntity{
    return MessageEntity(
        contents = msg.contents,
        contentString = msg.contents.joinToString { it.contentToString() },
        senderId = senderId,
        chatId = chatId,
        timestamp = msg.timestamp,
        sentByMe = false,
        verified = false,
        uid = msg.uuid,
    )
}