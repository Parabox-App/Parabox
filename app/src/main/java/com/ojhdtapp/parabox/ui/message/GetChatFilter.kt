package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat

sealed class GetChatFilter(
    val labelResId: Int,
    val check: (chat: Chat) -> Boolean
) {
    object Normal : GetChatFilter(
        labelResId = 1,
        check = {
            !it.isHidden
        }
    )

    object Archived : GetChatFilter(
        labelResId = 2,
        check = {
            it.isArchived
        }
    )

    object Hidden : GetChatFilter(
        labelResId = 3,
        check = {
            it.isHidden
        }
    )

    object Read : GetChatFilter(
        labelResId = 4,
        check = {
            it.unreadMessageNum == 0
        }
    )

    object Unread : GetChatFilter(
        labelResId = 5,
        check = {
            it.unreadMessageNum > 0
        }
    )

    object Private : GetChatFilter(
        labelResId = 7,
        check = {
            it.type == ParaboxChat.TYPE_PRIVATE
        }
    )

    object Group : GetChatFilter(
        labelResId = 8,
        check = {
            it.type == ParaboxChat.TYPE_GROUP
        }
    )

    class Tag(tag: String) : GetChatFilter(
        labelResId = 6,
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