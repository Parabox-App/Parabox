package com.ojhdtapp.parabox.domain.model

import android.util.Log
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent

data class ContactWithMessages(
    val contact: Contact,
    val messages: List<Message>
) {
    fun toTimedMessages(): Map<Long, List<ChatBlock>> = this.messages.toTimedMessages()
}
