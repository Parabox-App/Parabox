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
){
    override fun equals(other: Any?): Boolean {
        return if(other is Message){
            uid == other.uid
        } else super.equals(other)
    }
}
