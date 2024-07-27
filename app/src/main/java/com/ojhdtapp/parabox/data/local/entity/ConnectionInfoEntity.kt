package com.ojhdtapp.parabox.data.local.entity

import android.os.Bundle
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.ConnectionInfoType

@Entity(tableName = "connection_info_entity")

data class ConnectionInfoEntity(
    val alias: String,
    val name: String,
    val type: Int,
    val extra: Bundle,
    val pkg: String = "",
    val connectionClassName: String = "",
    val version: String = "",
    val versionCode: Long = 0,
    val builtInKey: String = "",
    @PrimaryKey(autoGenerate = true) val connectionId: Long = 0,
) {
    fun toConnectionInfo(): ConnectionInfo
    = ConnectionInfo(alias, name, ConnectionInfoType.entries[type], extra, pkg, connectionClassName, version, versionCode, builtInKey, connectionId)
}