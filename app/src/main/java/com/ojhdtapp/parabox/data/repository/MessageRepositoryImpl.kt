package com.ojhdtapp.parabox.data.repository

import android.content.Context
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : MessageRepository {
    override fun getMessageById(messageId: Long): Flow<Resource<Message>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(db.messageDao.getMessageById(messageId)?.toMessage()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("not found"))
            } catch (e: Exception) {
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getMessageWithLimit(limit: Int): Flow<Resource<List<QueryMessage>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    Resource.Success(db.messageDao.getMessageWithLimit(limit).map { it.toQueryMessage() })
                )
            } catch (e: Exception) {
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun queryMessage(query: String): Flow<Resource<List<QueryMessage>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    Resource.Success(db.messageDao.queryMessage(query).map { it.toQueryMessage() })
                )
            } catch (e: Exception) {
                emit(Resource.Error("unknown error"))
            }
        }
    }

}