package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Junction
import androidx.room.Relation

data class ContactWithMessages(
    val contact: ContactEntity,
    @Relation(
        parentColumn = "contactId",
        entityColumn = "messageId",
        associateBy = Junction(ContactMessageCrossRef::class)
    )
    val messages: List<MessageEntity>
)
