package com.ojhdtapp.parabox.data.local.entity

import android.os.Bundle
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.data.local.ExtensionInfo

@Entity(tableName = "extension_info_entity")

data class ExtensionInfoEntity(
    val alias: String,
    val pkg: String,
    val name: String,
    val version: String,
    val versionCode: Long,
    val extra: Bundle,
    @PrimaryKey(autoGenerate = true) val extensionId: Long = 0,
) {
    fun toExtensionInfo(): ExtensionInfo
    = ExtensionInfo(alias, pkg, name, version, versionCode, extra, extensionId)
}
