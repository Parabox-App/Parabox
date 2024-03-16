package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetChat @Inject constructor(
    val repository: ChatRepository
) {
    operator fun invoke(filter: List<ChatFilter>): Flow<PagingData<ChatWithLatestMessage>> {
        return Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                initialLoadSize = 20
            )
        ) { repository.getChatPagingSource(filter) }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toChatWithLatestMessage()
                }
//                    .filter { chatWithMsg ->
//                    filter.isEmpty() || filter.all { it.check(chatWithMsg.chat) }
//                }
            }
    }

    fun pinned(): Flow<PagingData<Chat>> {
        return Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                initialLoadSize = 20
            )
        ) { repository.getPinnedChatPagingSource() }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toChat()
                }
            }
    }

    fun containsContact(contactId: Long) : Flow<Resource<List<Chat>>> {
        return repository.containsContact(contactId)
    }

    fun withCustomTag(customTagChatFilter: ChatFilter.Tag) : Flow<Resource<List<Chat>>> {
        return repository.withCustomTag(customTagChatFilter)
    }
}