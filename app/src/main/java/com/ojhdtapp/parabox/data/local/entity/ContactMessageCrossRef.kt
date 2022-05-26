package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["contactId", "messageId"])
data class ContactMessageCrossRef(
    val contactId: Int,
    val messageId: Int
)
