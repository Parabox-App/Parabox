package com.ojhdtapp.parabox.domain.model


data class ChatBean(
    val chat: Chat,
    val message: Message?,
    val sender: Contact?
)
