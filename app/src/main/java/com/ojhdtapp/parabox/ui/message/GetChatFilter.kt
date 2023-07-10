package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Chat

sealed class GetChatFilter(
    val labelResId: Int,
    val check: (chat: Chat) -> Boolean
) {
    class Archived: GetChatFilter(
        labelResId = 1,
        check = {
            it.isArchived
        }
    )

    class Read: GetChatFilter(
        labelResId = 1,
        check = {
            it.unreadMessageNum == 0
        }
    )

    class Unread: GetChatFilter(
        labelResId = 1,
        check = {
            it.unreadMessageNum > 0
        }
    )

    class Tag(tag: String) : GetChatFilter(
        labelResId = -1,
        check = {
            it.tags.contains(tag)
        }
    )
}