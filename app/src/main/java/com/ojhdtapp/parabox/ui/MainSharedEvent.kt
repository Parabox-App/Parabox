package com.ojhdtapp.parabox.ui

import androidx.datastore.preferences.core.Preferences
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface MainSharedEvent : UiEvent {

    data class UpdateDataStore(val value: MainSharedState.DataStore) : MainSharedEvent
    data class QueryInput(val input: String) : MainSharedEvent
    data class SearchConfirm(val input: String) : MainSharedEvent
    data object GetRecentQuery : MainSharedEvent
    data class GetRecentQueryDone(val res: List<RecentQuery>, val isSuccess: Boolean) : MainSharedEvent
    data class DeleteRecentQuery(val id: Long) : MainSharedEvent
    data class MessageSearchDone(val res: List<QueryMessage>, val isSuccess: Boolean) :
        MainSharedEvent

    data class ContactSearchDone(val res: List<Contact>, val isSuccess: Boolean) : MainSharedEvent
    data class ChatSearchDone(val res: List<Chat>, val isSuccess: Boolean) : MainSharedEvent
    data class TriggerSearchBar(val isActive: Boolean) : MainSharedEvent
    data class OpenDrawer(val open: Boolean, val snap: Boolean = false) : MainSharedEvent
    data class OpenBottomSheet(val open: Boolean, val snap: Boolean = false) : MainSharedEvent
    object SearchAvatarClicked : MainSharedEvent
    data class ShowNavigationBar(val show: Boolean) : MainSharedEvent
    data class UpdateSearchDoneChatFilter(val filter: ChatFilter) : MainSharedEvent
    data class UpdateSearchDoneMessageFilter(val filter: MessageFilter) : MainSharedEvent
    data class PickChat(val onDone: (Chat?) -> Unit) : MainSharedEvent
    data class PickChatDone(val res: Chat?) : MainSharedEvent
    data class PickChatQueryInput(val input: String) : MainSharedEvent
    data class GetPickChatDone(val res: List<Chat>, val isSuccess: Boolean) : MainSharedEvent
    data class PickContact(val onDone: (Contact?) -> Unit) : MainSharedEvent
    data class PickContactDone(val res: Contact?) : MainSharedEvent
    data class PickContactQueryInput(val input: String) : MainSharedEvent
    data class GetPickContactDone(val res: List<Contact>, val isSuccess: Boolean) : MainSharedEvent
    data class PickDateRange(val onDone: (Pair<Long, Long>?) -> Unit) : MainSharedEvent
    data class PickDateRangeDone(val res: Pair<Long, Long>?) : MainSharedEvent
    data object PageListScrollBy : MainSharedEvent
    data class UpdateEnabledChatFilterList(val list: List<ChatFilter>) : MainSharedEvent
    data class OnChatFilterAdded(val filter: ChatFilter) : MainSharedEvent
    data class OnChatFilterRemoved(val filter: ChatFilter) : MainSharedEvent
    data class OnChatFilterListReordered(val fromIndex: Int, val toIndex: Int) : MainSharedEvent
    data class UpdateSettingSwitch(val key: Preferences.Key<Boolean>, val value: Boolean) : MainSharedEvent
    data class UpdateSettingMenu(val key: Preferences.Key<Int>, val value: Int) : MainSharedEvent
    data class LoadContactDetailDialog(val contactId: Long) : MainSharedEvent
    data class ShowContactDetailDialog(val contactWithExtensionInfo: ContactWithExtensionInfo) : MainSharedEvent
    data object DismissContactDetailDialog : MainSharedEvent
    data class UpdateContactRelativeChatList(val list: List<Chat>, val loadState: LoadState) : MainSharedEvent
}