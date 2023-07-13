package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatBeanEntity
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ChatRepository {
    override fun getChatPagingSource(): PagingSource<Int, ChatWithLatestMessageEntity> {
        return db.chatDao.getChatPagingSource()
    }

    override fun queryChat(query: String): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        Resource.Success(db.chatDao.queryChat(query).map { it.toChat() })
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }
}