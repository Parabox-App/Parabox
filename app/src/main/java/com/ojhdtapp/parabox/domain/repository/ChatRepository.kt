package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.data.local.entity.ChatBeanEntity
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage

interface ChatRepository {
    fun getChatPagingSource() : PagingSource<Int, ChatWithLatestMessageEntity>
}