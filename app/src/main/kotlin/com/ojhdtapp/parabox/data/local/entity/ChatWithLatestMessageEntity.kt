package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage

data class ChatWithLatestMessageEntity(
    @Embedded
    val chat: ChatEntity,
    @Relation(parentColumn = "latestMessageId", entityColumn = "messageId")
    val message: MessageEntity?
){
    fun toChatWithLatestMessage() : ChatWithLatestMessage{
        return ChatWithLatestMessage(
            chat.toChat(),
            message?.toMessage()
        )
    }
}
