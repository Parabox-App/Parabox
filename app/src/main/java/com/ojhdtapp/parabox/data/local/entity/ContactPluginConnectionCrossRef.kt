package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "contact_plugin_connection_cross_ref", primaryKeys = ["contactId", "objectId"], )
data class ContactPluginConnectionCrossRef(
    @ColumnInfo(index = true)
    val contactId: Long,
    @ColumnInfo(index = true)
    val objectId: Long,
)
