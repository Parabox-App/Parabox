package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.ui.base.UiState

data class MainSharedState(
    val search: Search = Search()
): UiState{
    data class Search(
        val query: String = "",
        val isActive: Boolean = false,
        val showRecent: Boolean = true,
        val message: MessageSearch = MessageSearch(),
        val contact: ContactSearch = ContactSearch(),
        val chat: ChatSearch = ChatSearch(),
    ){
        data class MessageSearch(
            val isLoading: Boolean = false,
            val isError: Boolean = false,
            val result: List<QueryMessage> = emptyList(),
            val filterResult: List<QueryMessage> = emptyList()
        )
        data class ContactSearch(
            val isLoading: Boolean = false,
            val isError: Boolean = false,
            val result: List<Contact> = emptyList(),
            val filterResult: List<Contact> = emptyList()
        )

        data class ChatSearch(
            val isLoading: Boolean = false,
            val isError: Boolean = false,
            val result: List<Chat> = emptyList(),
            val filterResult: List<Chat> = emptyList()
        )
    }
}
