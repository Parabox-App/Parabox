package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "contact_chat_cross_ref",primaryKeys = ["contactId", "chatId"])
data class ContactChatCrossRef(
    @ColumnInfo(index = true)
    val contactId: Long,
    @ColumnInfo(index = true)
    val chatId: Long,
)
