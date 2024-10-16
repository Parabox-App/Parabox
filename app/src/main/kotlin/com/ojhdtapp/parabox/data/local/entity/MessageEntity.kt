package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

@Entity(tableName = "message_entity")
data class MessageEntity(
    val contents: List<ParaboxMessageElement>,
    val contentTypes: Int,
    val contentString: String,
    val senderId: Long,
    val chatId: Long,
    val timestamp: Long,
    val sentByMe: Boolean,
    val verified: Boolean,
    val pkg: String,
    val uid: String,
    val extensionId: Long,
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
) {
    fun toMessage(): Message {
        return Message(
            contents,
            contentTypes,
            contentString,
            senderId,
            chatId,
            timestamp,
            sentByMe,
            verified,
            pkg,
            uid,
            extensionId,
            messageId
        )
    }
}

@Entity
data class MessageVerifyStateUpdate(
    @ColumnInfo(name = "messageId")
    val messageId: Long,
    @ColumnInfo(name = "verified")
    val verified: Boolean
)