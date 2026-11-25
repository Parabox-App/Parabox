package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import kotlinx.parcelize.Parcelize

@Parcelize
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
) : Parcelable {
    val contentsId: List<Long> get() = contents.indices.map {
        "${messageId}${it}".toLong()
    }
    override fun equals(other: Any?): Boolean {
        return if (other is Message) {
            messageId == other.messageId &&
                verified == other.verified &&
                uid == other.uid &&
                timestamp == other.timestamp
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + verified.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

fun List<Message>.contains(messageId: Long): Boolean{
    return map { it.messageId }.contains(messageId)
}