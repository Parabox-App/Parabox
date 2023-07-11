package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.ChatBean

data class ChatBeanEntity(
    @Embedded
    val chat: ChatEntity,
    @Relation(parentColumn = "latestMessageId", entityColumn = "messageId")
    val message: MessageEntity?,
    @Relation(parentColumn = "senderId", entityColumn = "contactId")
    val sender: ContactEntity?
) {
    fun toChatBean(): ChatBean {
        return ChatBean(
            chat.toChat(), message?.toMessage(), sender?.toContact()
        )
    }
}
