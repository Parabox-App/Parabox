package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ChatRepository {
    override fun getChatPagingSource(): PagingSource<Int, ChatWithLatestMessageEntity> {
        return db.chatDao.getChatPagingSource()
    }
}