package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

data class Chat (
    val name: String,
    val avatar: ParaboxResourceInfo,
    val latestMessageId: Long?,
    val isHidden: Boolean = false,
    val isPinned : Boolean = false,
    val isArchived: Boolean = false,
    val isNotificationEnabled : Boolean = true,
    val tags: List<String>,
    val subChatIds: List<Long>,
    val pkg: String,
    val uid: String,
    val chatId: Long,
)
