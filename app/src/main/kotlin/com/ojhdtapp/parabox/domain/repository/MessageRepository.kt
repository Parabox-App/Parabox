package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.MessageWithSenderEntity
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagePagingSource(chatIdList: List<Long>, filter: List<MessageFilter>): PagingSource<Int, MessageWithSenderEntity>
    fun getMessageById(messageId: Long): Flow<Resource<Message>>
    fun getMessageWithLimit(limit: Int): Flow<Resource<List<QueryMessage>>>
    fun queryMessageWithLimit(query: String, limit: Int): Flow<Resource<List<QueryMessage>>>
}