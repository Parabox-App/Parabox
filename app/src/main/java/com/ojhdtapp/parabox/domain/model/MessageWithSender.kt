package com.ojhdtapp.parabox.domain.model

data class MessageWithSender(
    val message: Message,
    val sender: Contact
)
