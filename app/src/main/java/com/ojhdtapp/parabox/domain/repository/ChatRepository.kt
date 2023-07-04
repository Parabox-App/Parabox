package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity

interface ChatRepository {
    fun getChatPagingSource() : PagingSource<Int, ChatWithLatestMessageEntity>
}