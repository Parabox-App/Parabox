package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

@Entity(tableName = "contact_entity")
data class ContactEntity(
    val name: String,
    val avatar: ParaboxResourceInfo,
    val latestMessageId: Long?,
    val isHidden: Boolean = false,
    val isPinned : Boolean = false,
    val isArchived: Boolean = false,
    val isNotificationEnabled : Boolean = true,
    val type: Int,
    val tags: List<String>,
    val pkg: String,
    val uid: String,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
)