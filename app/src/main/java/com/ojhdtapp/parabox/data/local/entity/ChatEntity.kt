package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

@Entity(tableName = "chat_entity")
data class ChatEntity(
    val name: String,
    val avatar: ParaboxResourceInfo,
    val latestMessageId: Long?,
    val unreadMessageNum: Int = 0,
    val isHidden: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isNotificationEnabled: Boolean = true,
    val tags: List<String>,
    val subChatIds: List<Long>,
    val pkg: String,
    val uid: String,
    @PrimaryKey(autoGenerate = true) val chatId: Long = 0,
) {
    fun toChat(): Chat {
        return Chat(
            name,
            avatar,
            latestMessageId,
            unreadMessageNum,
            isHidden,
            isPinned,
            isArchived,
            isNotificationEnabled,
            tags,
            subChatIds,
            pkg,
            uid,
            chatId
        )
    }
}

@Entity
data class ChatLatestMessageIdUpdate(
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "latestMessageId")
    val latestMessageId: Long?,
)

@Entity
data class ChatUnreadMessagesNumUpdate(
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "unreadMessageNum")
    val unreadMessageNum: Int,
)