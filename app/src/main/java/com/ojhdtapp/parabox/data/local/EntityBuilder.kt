package com.ojhdtapp.parabox.data.local

import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

fun buildContactEntity(msg: ReceiveMessage, ext: ExtensionInfo): ContactEntity {
    return ContactEntity(
        name = msg.sender.basicInfo.name,
        avatar = msg.sender.basicInfo.avatar,
        pkg = ext.pkg,
        uid = msg.sender.uid,
        extensionId = ext.extensionId
    )
}

fun buildChatEntity(msg: ReceiveMessage, ext: ExtensionInfo): ChatEntity {
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
        extensionId = ext.extensionId
    )
}

fun buildMessageEntity(msg: ReceiveMessage, ext: ExtensionInfo, senderId: Long, chatId: Long): MessageEntity {
    val typeList = msg.contents.map { it.getType() }
    val contentTypes = buildString {
        (0 until (typeList.maxOrNull() ?: 0)).forEach {
            if (it in typeList) {
                insert(0, 1)
            } else insert(0, 0)
        }
    }.ifBlank { "0" }.toInt(2)
    return MessageEntity(
        contents = msg.contents,
        contentTypes = contentTypes,
        contentString = msg.contents.joinToString { it.contentToString() },
        senderId = senderId,
        chatId = chatId,
        timestamp = msg.timestamp,
        sentByMe = false,
        verified = false,
        pkg = ext.pkg,
        uid = msg.uuid,
        extensionId = ext.extensionId
    )
}