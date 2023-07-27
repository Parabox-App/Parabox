package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface MessagePageEvent : UiEvent {
    object UpdateDataStore : MessagePageEvent
    data class OpenEnabledChatFilterDialog(val open: Boolean) : MessagePageEvent
    data class UpdateEnabledGetChatFilterList(val list: List<GetChatFilter>) : MessagePageEvent
    data class AddOrRemoveSelectedGetChatFilter(val filter: GetChatFilter) : MessagePageEvent
    data class GetChatPagingDataFlow(val filterList: List<GetChatFilter>) : MessagePageEvent
    data class UpdateChatUnreadMessagesNum(val chatId: Long, val value: Int, val oldValue: Int) : MessagePageEvent
    data class UpdateChatHide(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatPin(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatArchive(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatTags(val chatId: Long, val value: List<String>, val oldValue: List<String>) : MessagePageEvent
    data class UpdateEditingChatTags(val obj: ChatTagsUpdate?): MessagePageEvent
}
