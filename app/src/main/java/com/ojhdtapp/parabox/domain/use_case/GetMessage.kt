package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessage @Inject constructor(
    val repository: MessageRepository
) {
    operator fun invoke(chatIdList: List<Long>): Flow<PagingData<Message>> {
        return Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                initialLoadSize = 20
            )
        ) { repository.getMessagePagingSource(chatIdList) }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toMessage()
                }
            }
    }

    fun byId(messageId: Long): Flow<Resource<Message>> {
        return repository.getMessageById(messageId)
    }
}