package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.QueryMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessageById(messageId: Long): Flow<Resource<Message>>
    fun getMessageWithLimit(limit: Int): Flow<Resource<List<QueryMessage>>>
    fun queryMessageWithLimit(query: String, limit: Int): Flow<Resource<List<QueryMessage>>>
}