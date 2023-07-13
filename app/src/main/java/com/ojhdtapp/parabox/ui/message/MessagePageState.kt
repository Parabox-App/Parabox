package com.ojhdtapp.parabox.ui.message

import androidx.paging.PagingData
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.ui.base.UiState
import kotlinx.coroutines.flow.Flow

data class MessagePageState(
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>>,
    val enabledGetChatFilterList: List<GetChatFilter> = emptyList(),
    val selectedGetChatFilterList: List<GetChatFilter> = listOf(GetChatFilter.Normal),
    val datastore: DataStore = DataStore(),
    val openEnabledChatFilterDialog: Boolean = false,
): UiState{
    data class DataStore(
        val enableSwipeToDismiss: Boolean = false
    )
}