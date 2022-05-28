package com.ojhdtapp.parabox.domain.model

data class ContactWithMessages(
    val contact: Contact,
    val messages: List<Message>
)
