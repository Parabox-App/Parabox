package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.QueryMessage

data class QueryMessageEntity(
    @Embedded
    val message: MessageEntity,
    @Relation(parentColumn = "chatId", entityColumn = "chatId")
    val chat: ChatEntity?,
    @Relation(parentColumn = "senderId", entityColumn = "contactId")
    val contact: ContactEntity?
) {
    fun toQueryMessage(): QueryMessage {
        return QueryMessage(
            message.toMessage(), chat?.toChat(), contact?.toContact()
        )
    }
}
