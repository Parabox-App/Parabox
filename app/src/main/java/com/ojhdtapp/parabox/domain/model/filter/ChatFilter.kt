package com.ojhdtapp.parabox.domain.model.filter

import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat

sealed class ChatFilter(
    open val label: String? = null,
    val labelResId: Int,
    val descriptionResId: Int,
    val key: String,
    val check: (chat: Chat) -> Boolean
) {
    object Normal : ChatFilter(
        labelResId = R.string.get_chat_filter_normal,
        descriptionResId = R.string.get_chat_filter_normal,
        key = "_#parabox_chat_filter_normal",
        check = {
            !it.isHidden && !it.isPinned && !it.isArchived
        }
    )

    object Archived : ChatFilter(
        labelResId = R.string.get_chat_filter_archived,
        descriptionResId = R.string.get_chat_filter_archived_des,
        key = "_#parabox_chat_filter_archived",
        check = {
            it.isArchived
        }
    )

    object Hidden : ChatFilter(
        labelResId = R.string.get_chat_filter_hidden,
        descriptionResId = R.string.get_chat_filter_hidden_des,
        key = "_#parabox_chat_filter_hidden",
        check = {
            it.isHidden
        }
    )

    object Read : ChatFilter(
        labelResId = R.string.get_chat_filter_read,
        descriptionResId = R.string.get_chat_filter_read_des,
        key = "_#parabox_chat_filter_read",
        check = {
            it.unreadMessageNum == 0
        }
    )

    object Unread : ChatFilter(
        labelResId = R.string.get_chat_filter_unread,
        descriptionResId = R.string.get_chat_filter_unread_des,
        key = "_#parabox_chat_filter_unread",
        check = {
            it.unreadMessageNum > 0
        }
    )

    object Private : ChatFilter(
        labelResId = R.string.get_chat_filter_private,
        descriptionResId = R.string.get_chat_filter_private_des,
        key = "_#parabox_chat_filter_private",
        check = {
            it.type == ParaboxChat.TYPE_PRIVATE
        }
    )

    object Group : ChatFilter(
        labelResId = R.string.get_chat_filter_group,
        descriptionResId = R.string.get_chat_filter_group_des,
        key = "_#parabox_chat_filter_group",
        check = {
            it.type == ParaboxChat.TYPE_GROUP
        }
    )

    class Tag(override val label: String) : ChatFilter(
        labelResId = -1,
        descriptionResId = -1,
        key = label,
        check = {
            it.tags.contains(label)
        }
    )

    override fun equals(other: Any?): Boolean {
        return labelResId == (other as? ChatFilter)?.labelResId
    }
    companion object{
        fun fromKey(key: String): ChatFilter? {
            return if (key.startsWith("_#parabox_chat_filter_")) {
                allFilterList.find { it.key == key }
            } else {
                Tag(key)
            }
        }
        val allFilterList = listOf<ChatFilter>(
            Archived, Hidden, Read, Unread, Private, Group
        )
    }
}