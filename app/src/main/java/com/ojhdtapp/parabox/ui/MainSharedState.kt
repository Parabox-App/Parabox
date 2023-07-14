package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.ui.base.UiState

data class MainSharedState(
    val search: Search = Search()
): UiState{
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
            val filterResult: List<QueryMessage> = emptyList()
        )
        data class ContactSearch(
            val loadState: LoadState = LoadState.LOADING,
            val result: List<Contact> = emptyList(),
            val filterResult: List<Contact> = emptyList()
        )

        data class ChatSearch(
            val loadState: LoadState = LoadState.LOADING,
            val result: List<Chat> = emptyList(),
            val filterResult: List<Chat> = emptyList()
        )
    }
}
