package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement

@Entity(tableName = "message_entity")
data class MessageEntity(
    val contents: List<ParaboxMessageElement>,
    val contentString: String,
    val senderId: Long,
    val timestamp: Long,
    val sentByMe: Boolean,
    val verified : Boolean,
    val uid: String,
    @PrimaryKey(autoGenerate = true) val messageId: Long,
) {
}

@Entity
data class MessageVerifyStateUpdate(
    @ColumnInfo(name = "messageId")
    val messageId: Long,
    @ColumnInfo(name = "verified")
    val verified: Boolean
)