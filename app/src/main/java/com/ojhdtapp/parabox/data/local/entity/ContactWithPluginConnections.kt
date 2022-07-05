package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ContactWithPluginConnections(
    @Embedded val contact: ContactEntity,
    @Relation(
        parentColumn = "contactId",
        entityColumn = "objectId",
        associateBy = Junction(ContactPluginConnectionCrossRef::class)
    )
    val pluginConnectionList: List<PluginConnectionEntity>
)
