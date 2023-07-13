package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface MessagePageEvent : UiEvent {
    object UpdateDataStore : MessagePageEvent
    data class OpenEnabledChatFilterDialog(val open: Boolean) : MessagePageEvent
    data class UpdateEnabledGetChatFilterList(val list: List<GetChatFilter>) : MessagePageEvent
    data class AddOrRemoveSelectedGetChatFilter(val filter: GetChatFilter) : MessagePageEvent
    data class GetChatPagingDataFlow(val filterList: List<GetChatFilter>) : MessagePageEvent
}
