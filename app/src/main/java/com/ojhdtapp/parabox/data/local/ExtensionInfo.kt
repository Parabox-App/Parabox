package com.ojhdtapp.parabox.data.local

import android.os.Bundle
import androidx.room.PrimaryKey

data class ExtensionInfo(
    val alias: String,
    val name: String,
    val type: ExtensionInfoType,
    val extra: Bundle,
    val pkg: String = "",
    val version: String = "",
    val versionCode: Long = 0,
    val builtInKey: String = "",
    val extensionId: Long = 0
)

enum class ExtensionInfoType{
    Extend, BuiltIn
}