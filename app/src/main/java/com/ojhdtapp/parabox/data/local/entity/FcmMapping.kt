package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FcmMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val from: String,
    val uid: String,
    val sessionId: String
)

@Entity
data class FcmMappingSessionIdUpdate(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "sessionId")
    val sessionId: String
)
