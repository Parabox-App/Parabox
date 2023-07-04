package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

data class Message(
    val contents: List<ParaboxMessageElement>,
    val contentTypes: Int,
    val contentString: String,
    val senderId: Long,
    val chatId: Long,
    val timestamp: Long,
    val sentByMe: Boolean,
    val verified : Boolean,
    val uid: String,
    val messageId: Long,
)
