package com.ojhdtapp.parabox.domain.model

data class ChatWithLatestMessage(
    val chat: Chat,
    val message: Message?
)
