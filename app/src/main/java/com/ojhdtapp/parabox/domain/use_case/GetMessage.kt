package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessage @Inject constructor(
    val repository: MessageRepository
) {
    operator fun invoke(chatIdList: List<Long>, filter: List<MessageFilter>): Flow<PagingData<ChatPageUiModel>> {
        return Pager(
            PagingConfig(
                pageSize = 40,
                prefetchDistance = 40,
                enablePlaceholders = true,
                initialLoadSize = 40
            )
        ) {
            repository.getMessagePagingSource(
                chatIdList, filter,
            )
        }
            .flow
            .map { pagingData ->
                pagingData.map {
                    it.toMessageWithSender()
                }
            }.map { it.insertSeparators { before: ChatPageUiModel.MessageWithSender?, after: ChatPageUiModel.MessageWithSender? ->
                if((before?.message?.timestamp?:0) - (after?.message?.timestamp ?: 0) > 180000){
                    ChatPageUiModel.Divider(before!!.message.timestamp)
                } else {
                    null
                }
            } }
    }

    fun byId(messageId: Long): Flow<Resource<Message>> {
        return repository.getMessageById(messageId)
    }
}