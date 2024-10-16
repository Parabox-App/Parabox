package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.MessageWithSenderEntity
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : MessageRepository {
    override fun getMessagePagingSource(
        chatIdList: List<Long>,
        filter: List<MessageFilter>
    ): PagingSource<Int, MessageWithSenderEntity> {
        val queryStr = buildString {
            append("SELECT message_entity.* FROM message_entity ")
            append("WHERE chatId IN (${chatIdList.joinToString(",")}) ")
            filter.forEachIndexed { index, messageFilter ->
                append("AND ")
                when (messageFilter) {
                    is MessageFilter.ContentFilter -> {
                        append("contentString LIKE '%{${messageFilter.queryString}}%' ")
                    }

                    is MessageFilter.ContentTypeFilter -> {
                        append("contentTypes & ${messageFilter.andValue} ")
                    }

                    is MessageFilter.DateFilter.WithinThreeDays -> {
                        append("timestamp + 259200000 > ${System.currentTimeMillis()} ")
                    }

                    is MessageFilter.DateFilter.WithinThisWeek -> {
                        append("timestamp + 604800000 > ${System.currentTimeMillis()} ")
                    }

                    is MessageFilter.DateFilter.WithinThisMonth -> {
                        append("timestamp + 2592000000 > ${System.currentTimeMillis()} ")
                    }

                    is MessageFilter.DateFilter.MoreThanAMonth -> {
                        append("timestamp + 2592000000 < ${System.currentTimeMillis()} ")
                    }

                    is MessageFilter.DateFilter.Custom -> {
                        append("timestamp > ${messageFilter.timestampStart} AND timestamp < ${messageFilter.timestampEnd} ")
                    }

                    is MessageFilter.SenderFilter.Custom -> {
                        append("senderId = ${messageFilter.senderId} ")
                    }

                    is MessageFilter.ChatFilter.Custom -> {
                        append("chatId = ${messageFilter.chatId} ")
                    }

                    else -> {

                    }
                }
            }
            append("ORDER BY message_entity.timestamp DESC")
        }
        val query = SimpleSQLiteQuery(queryStr)
        return db.messageDao.getMessagePagingSource(query)
    }

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
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun queryMessageWithLimit(query: String, limit: Int): Flow<Resource<List<QueryMessage>>> {
        return flow {
            emit(Resource.Loading())
            try {
                if (limit == 0) {
                    emit(
                        Resource.Success(db.messageDao.queryMessage(query).map { it.toQueryMessage() })
                    )
                } else {
                    emit(
                        Resource.Success(db.messageDao.queryMessageWithLimit(query, limit).map { it.toQueryMessage() })
                    )
                }
            } catch (e: Exception) {
                emit(Resource.Error("unknown error"))
            }
        }
    }

}