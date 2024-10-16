package com.ojhdtapp.parabox.domain.model

data class QueryMessage(
    val message: Message,
    val chat: Chat?,
    val contact: Contact?
)
