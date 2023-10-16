package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel

data class MessageWithSenderEntity(
    @Embedded
    val message: MessageEntity,
    @Relation(parentColumn = "senderId", entityColumn = "contactId")
    val sender: ContactEntity
){
    fun toMessageWithSender(): ChatPageUiModel.MessageWithSender {
        return ChatPageUiModel.MessageWithSender(
            message.toMessage(),
            sender.toContact()
        )
    }
}
