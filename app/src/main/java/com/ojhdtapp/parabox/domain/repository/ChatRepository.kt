package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ChatBeanEntity
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatPagingSource() : PagingSource<Int, ChatWithLatestMessageEntity>

    fun queryChat(query: String): Flow<Resource<List<Chat>>>
}