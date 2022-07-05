package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity(tableName = "plugin_connection_entity")
data class PluginConnectionEntity(
    val connectionType: Int,
    @PrimaryKey val objectId: Long
){
    fun toPluginConnection() : PluginConnection{
        return PluginConnection(
            connectionType = connectionType,
            objectId = objectId
        )
    }
}
