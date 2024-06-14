package com.ojhdtapp.parabox.data.local.entity

import android.os.Bundle
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.ExtensionInfoType

@Entity(tableName = "extension_info_entity")

data class ExtensionInfoEntity(
    val alias: String,
    val name: String,
    val type: Int,
    val extra: Bundle,
    val pkg: String = "",
    val version: String = "",
    val versionCode: Long = 0,
    val builtInKey: String = "",
    @PrimaryKey(autoGenerate = true) val extensionId: Long = 0,
) {
    fun toExtensionInfo(): ExtensionInfo
    = ExtensionInfo(alias, name, ExtensionInfoType.entries[type], extra, pkg, version, versionCode, builtInKey, extensionId)
}