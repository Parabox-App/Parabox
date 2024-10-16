package com.ojhdtapp.parabox.data.local

import android.os.Bundle
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import org.json.JSONObject

data class ConnectionInfo(
    val alias: String,
    val name: String,
    val type: ConnectionInfoType,
    val extra: JSONObject,
    val pkg: String = "",
    val connectionClassName: String = "",
    val version: String = "",
    val versionCode: Long = 0,
    val key: String = "",
    val connectionId: Long = 0
) {
    fun toConnectionInfoEntity(): ConnectionInfoEntity
    = ConnectionInfoEntity(alias, name, type.ordinal, extra, pkg, connectionClassName, version, versionCode, key, connectionId)
}

enum class ConnectionInfoType{
    Extend, BuiltIn
}