package com.ojhdtapp.parabox.domain.model.chat

import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.Profile

data class ChatBlock(
    val profile: Profile,
    val messages: List<Message>
)
