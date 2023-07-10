package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.ui.message.GetChatFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetChat @Inject constructor(
    val repository: ChatRepository
) {
    operator fun invoke(filter: List<GetChatFilter>): Flow<PagingData<ChatWithLatestMessage>> {
        return Pager(
            PagingConfig(
                pageSize = 20,
            )
        ) { repository.getChatPagingSource() }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toChatWithLatestMessage()
                }.filter { chatWithMsg ->
                    filter.all { it.check(chatWithMsg.chat) }
                }
            }
    }
}