package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent

@Entity(tableName = "message_entity")
data class MessageEntity(
    val contents: List<MessageContent>,
    @Embedded val profile: Profile,
    val timestamp: Long,
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val sentByMe: Boolean,
    val verified : Boolean,
) {
    fun toMessage() = Message(
        contents = contents,
        profile = profile,
        timestamp = timestamp,
        messageId = messageId,
        sentByMe = sentByMe,
        verified = verified,
    )
}

@Entity
data class MessageVerifyStateUpdate(
    @ColumnInfo(name = "messageId")
    val messageId: Long,
    @ColumnInfo(name = "verified")
    val verified: Boolean
)