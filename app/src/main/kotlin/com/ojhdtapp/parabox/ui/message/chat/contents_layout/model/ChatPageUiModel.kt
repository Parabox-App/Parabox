package com.ojhdtapp.parabox.ui.message.chat.contents_layout.model

import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message

sealed interface ChatPageUiModel {
    val id: Long
    data class MessageWithSender(
        val message: Message,
        val sender: Contact
    ) : ChatPageUiModel {
        override val id: Long
            get() = message.messageId
    }

    data class Divider(val timestamp: Long) : ChatPageUiModel {
        override val id: Long
            get() = timestamp
    }
}