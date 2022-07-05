package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PluginConnectionWithContacts(
    @Embedded val pluginConnection: PluginConnectionEntity,
    @Relation(
        parentColumn = "objectId",
        entityColumn = "contactId",
        associateBy = Junction(ContactPluginConnectionCrossRef::class)
    )
    val contactList: List<ContactEntity>
)
