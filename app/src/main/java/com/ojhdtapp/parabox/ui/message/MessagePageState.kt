package com.ojhdtapp.parabox.ui.message

import androidx.paging.PagingData
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState
import kotlinx.coroutines.flow.Flow

data class MessagePageState(
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>>,
    val pinnedChatPagingDataFlow: Flow<PagingData<Chat>>,
    val currentChat: Chat? = null,
    val messagePagingDataFlow: Flow<PagingData<Message>>,
    val enabledChatFilterList: List<ChatFilter> = emptyList(),
    val selectedChatFilterLists: List<ChatFilter> = listOf(ChatFilter.Normal),
    val datastore: DataStore = DataStore(),
    val openEnabledChatFilterDialog: Boolean = false,
    val editingChatTags: ChatTagsUpdate? = null
): UiState{
    data class DataStore(
        val enableSwipeToDismiss: Boolean = false
    )
}