package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

@Entity(tableName = "chat_entity")
data class ChatEntity(
    val name: String,
    val avatar: ParaboxResourceInfo,
    val latestMessageId: Long?,
    val isHidden: Boolean = false,
    val isPinned : Boolean = false,
    val isArchived: Boolean = false,
    val isNotificationEnabled : Boolean = true,
    val tags: List<String>,
    val pkg: String,
    val uid: String,
    @PrimaryKey(autoGenerate = true) val chatId: Long = 0,
)
