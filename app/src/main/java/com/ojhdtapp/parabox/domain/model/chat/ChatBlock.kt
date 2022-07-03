package com.ojhdtapp.parabox.domain.model.chat

import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent

data class ChatBlock(
    val profile: Profile,
    val content: List<List<MessageContent>>
)
