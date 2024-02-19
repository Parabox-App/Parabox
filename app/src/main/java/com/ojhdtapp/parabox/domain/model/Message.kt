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
    val pkg: String,
    val uid: String,
    val extensionId: Long,
    val messageId: Long,
){
    val contentsId: List<Long> get() = contents.indices.map {
        "${messageId}${it}".toLong()
    }
    override fun equals(other: Any?): Boolean {
        return if(other is Message){
            messageId == other.messageId
        } else super.equals(other)
    }
}

fun List<Message>.contains(messageId: Long): Boolean{
    return map { it.messageId }.contains(messageId)
}