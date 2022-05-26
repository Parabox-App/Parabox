package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent

data class Message(
    val contents: List<MessageContent>,
    val profile: MessageProfile,
    val timestamp: Long
){
    fun toMessageEntity(id: Int) : MessageEntity{
        return MessageEntity(
            contents = contents,
            profile = profile,
            timestamp = timestamp,
            messageId = id
        )
    }
}
