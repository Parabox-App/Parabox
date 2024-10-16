package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class Chat (
    val avatar: ParaboxResourceInfo,
    val latestMessageId: Long?,
    val unreadMessageNum: Int = 0,
    val isHidden: Boolean = false,
    val isPinned : Boolean = false,
    val isArchived: Boolean = false,
    val isNotificationEnabled : Boolean = true,
    val tags: List<String>,
    val subChatIds: List<Long>,
    val type: Int,
    val pkg: String,
    val uid: String,
    val extensionId: Long?,
    val chatId: Long,
) : Parcelable {
    var name: String = ""
        get() = field.ifBlank { uid }
    constructor(
        name: String?,
        avatar: ParaboxResourceInfo,
        latestMessageId: Long?,
        unreadMessageNum: Int = 0,
        isHidden: Boolean = false,
        isPinned : Boolean = false,
        isArchived: Boolean = false,
        isNotificationEnabled : Boolean = true,
        tags: List<String>,
        subChatIds: List<Long>,
        type: Int,
        pkg: String,
        uid: String,
        extensionId: Long?,
        chatId: Long,
    ) : this(avatar, latestMessageId, unreadMessageNum, isHidden, isPinned, isArchived, isNotificationEnabled, tags, subChatIds, type, pkg, uid, extensionId, chatId) {
        this.name = name ?: ""
    }
}
