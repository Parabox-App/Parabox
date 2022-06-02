package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.ContactWithMessages

data class ContactWithMessagesEntity(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "contactId",
        entityColumn = "messageId",
        associateBy = Junction(ContactMessageCrossRef::class)
    )
    val messages: List<MessageEntity>
) {
    fun toContactWithMessages(): ContactWithMessages {
        return ContactWithMessages(
            contact = contact.toContact(),
            messages = messages.map { it.toMessage() }
        )
    }
}
