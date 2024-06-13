package com.ojhdtapp.parabox.ui

import android.net.Uri
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import com.ojhdtapp.parabox.ui.base.UiState
import com.ojhdtapp.parabox.ui.theme.Theme

data class MainSharedState(
    val datastore: DataStore = DataStore(),
    val search: Search = Search(),
    val openDrawer:OpenDrawer = OpenDrawer(),
    val openBottomSheet: OpenBottomSheet = OpenBottomSheet(),
    val showNavigationBar: Boolean = false,
    val openMainDialog: Boolean = false,
    val contactPicker: ContactPicker = ContactPicker(),
    val chatPicker: ChatPicker = ChatPicker(),
    val openDateRangePicker: Boolean = false,
    val contactDetailDialogState: ContactDetailDialogState = ContactDetailDialogState(),
): UiState{
    data class DataStore(
        val messageBadgeNum: Int = 0,
        val localName: String = "User",
        val localAvatarUri: Uri = Uri.EMPTY,
        val enabledChatFilterList: List<ChatFilter> = emptyList(),

        val enableMarqueeEffectOnChatName: Boolean = true,
        val enableSwipeToDismiss: Boolean = true,
        val displayAvatarOnTopAppBar: Boolean = true,
        val displayTimeOnEachMsg: Boolean = false,
        val enableInnerBrowser: Boolean = true,
        val sendViaEnter: Boolean = false,
        val enableDynamicColor: Boolean = true,
        val enableForegroundNotification: Boolean = false,
        val theme: Theme = Theme.WILLOW,
        val darkMode: DataStoreKeys.DarkMode = DataStoreKeys.DarkMode.FOLLOW_SYSTEM,
    )
    data class Search(
        val query: String = "",
        val recentQuery: List<RecentQuery> = emptyList(),
        val recentQueryState: LoadState = LoadState.LOADING,
        val isActive: Boolean = false,
        val showRecent: Boolean = true,
        val message: MessageSearch = MessageSearch(),
        val contact: ContactSearch = ContactSearch(),
        val chat: ChatSearch = ChatSearch(),
    ){
        data class MessageSearch(
            val loadState: LoadState = LoadState.LOADING,
            val result: List<QueryMessage> = emptyList(),
            val filterList: List<MessageFilter> = listOf(MessageFilter.SenderFilter.All, MessageFilter.ChatFilter.All, MessageFilter.DateFilter.All),
            val filterResult: List<QueryMessage> = emptyList()
        )
        data class ContactSearch(
            val loadState: LoadState = LoadState.LOADING,
            val result: List<Contact> = emptyList(),
        )

        data class ChatSearch(
            val loadState: LoadState = LoadState.LOADING,
            val result: List<Chat> = emptyList(),
            val enabledFilterList: List<ChatFilter> = emptyList(),
            val filterResult: List<Chat> = emptyList()
        )
    }

    data class OpenDrawer(
        val open: Boolean = false,
        val snap: Boolean = false,
    )

    data class OpenBottomSheet(
        val open: Boolean = false,
        val snap: Boolean = false,
    )

    data class ContactPicker(
        val showDialog: Boolean = false,
        val loadState: LoadState = LoadState.LOADING,
        val query: String = "",
        val result: List<Contact> = emptyList(),
    )

    data class ChatPicker(
        val showDialog: Boolean = false,
        val loadState: LoadState = LoadState.LOADING,
        val query: String = "",
        val result: List<Chat> = emptyList(),
    )

    data class ContactDetailDialogState(
        val contactWithExtensionInfo: ContactWithExtensionInfo? = null,
        val relativeChatList: List<Chat> = emptyList(),
        val loadState: LoadState = LoadState.LOADING,
    )
}
