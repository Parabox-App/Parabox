package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FcmMapping(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val from: String,
    val uid: String
)
