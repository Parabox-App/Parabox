package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat

sealed class GetChatFilter(
    val labelResId: Int,
    val check: (chat: Chat) -> Boolean
) {
    object Normal : GetChatFilter(
        labelResId = R.string.get_chat_filter_normal,
        check = {
            !it.isHidden
        }
    )

    object Archived : GetChatFilter(
        labelResId = R.string.get_chat_filter_archived,
        check = {
            it.isArchived
        }
    )

    object Hidden : GetChatFilter(
        labelResId = R.string.get_chat_filter_hidden,
        check = {
            it.isHidden
        }
    )

    object Read : GetChatFilter(
        labelResId = R.string.get_chat_filter_read,
        check = {
            it.unreadMessageNum == 0
        }
    )

    object Unread : GetChatFilter(
        labelResId = R.string.get_chat_filter_unread,
        check = {
            it.unreadMessageNum > 0
        }
    )

    object Private : GetChatFilter(
        labelResId = R.string.get_chat_filter_private,
        check = {
            it.type == ParaboxChat.TYPE_PRIVATE
        }
    )

    object Group : GetChatFilter(
        labelResId = R.string.get_chat_filter_group,
        check = {
            it.type == ParaboxChat.TYPE_GROUP
        }
    )

    class Tag(val tag: String) : GetChatFilter(
        labelResId = -1,
        check = {
            it.tags.contains(tag)
        }
    )



    override fun equals(other: Any?): Boolean {
        return labelResId == (other as? GetChatFilter)?.labelResId
    }
    companion object{
        val allFilterList = listOf<GetChatFilter>(
            Archived, Hidden, Read, Unread, Private, Group
        )
    }
}