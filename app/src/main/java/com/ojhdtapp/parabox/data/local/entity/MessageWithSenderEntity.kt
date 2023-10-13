package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.MessageWithSender

data class MessageWithSenderEntity(
    @Embedded
    val message: MessageEntity,
    @Relation(parentColumn = "senderId", entityColumn = "contactId")
    val sender: ContactEntity
){
    fun toMessageWithSender(): MessageWithSender {
        return MessageWithSender(
            message.toMessage(),
            sender.toContact()
        )
    }
}
