package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatArchiveUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatBeanEntity
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ChatHideUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatNotificationEnabledUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatPinUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : ChatRepository {
    override fun getChatPagingSource(filter: List<ChatFilter>): PagingSource<Int, ChatWithLatestMessageEntity> {
        val queryStr = buildString {
            append("SELECT chat_entity.* FROM chat_entity ")
            append("INNER JOIN message_entity ON chat_entity.latestMessageId = message_entity.messageId ")
            if (filter.isNotEmpty()) {
                append("WHERE ")
            }
            filter.forEachIndexed { index, chatFilter ->
                if (index > 0) {
                    append("AND ")
                }
                when (chatFilter) {
                    is ChatFilter.Normal -> {
                        append("NOT isHidden AND NOT isArchived ")
                    }

                    is ChatFilter.Archived -> {
                        append("isArchived ")
                    }

                    is ChatFilter.Group -> {
                        append("type = 0 ")
                    }

                    is ChatFilter.Hidden -> {
                        append("isHidden ")
                    }

                    is ChatFilter.Private -> {
                        append("type = 1 ")
                    }

                    is ChatFilter.Read -> {
                        append("unreadMessageNum = 0 ")
                    }

                    is ChatFilter.Unread -> {
                        append("unreadMessageNum > 0 ")
                    }

                    is ChatFilter.Tag -> {
                        append("tags LIKE '%${chatFilter.label}%' ")
                    }
                }
            }
            append("ORDER BY message_entity.timestamp DESC")
        }
        val query = SimpleSQLiteQuery(queryStr)
        return db.chatDao.getChatPagingSource(query)
    }

    override fun getPinnedChatPagingSource(): PagingSource<Int, ChatEntity> {
        return db.chatDao.getPinnedChatPagingSource()
    }

    override fun queryChatWithLimit(query: String, limit: Int): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                if (limit == 0) {
                    emit(
                        Resource.Success(db.chatDao.queryChat(query).map { it.toChat() })
                    )
                } else {
                    emit(
                        Resource.Success(db.chatDao.queryChatWithLimit(query, limit).map { it.toChat() })
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getChatWithLimit(limit: Int): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        Resource.Success(db.chatDao.getChatWithLimit(limit).map { it.toChat() })
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun getNotificationDisabledChat(): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
               emitAll(
                   db.chatDao.getNotificationDisabledChats().map {
                       Resource.Success(it.map { it.toChat() })
                   }
               )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override fun updateUnreadMessagesNum(chatId: Long, value: Int): Boolean {
        return db.chatDao.updateUnreadMessageNum(ChatUnreadMessagesNumUpdate(chatId, value)) == 1
    }

    override fun updatePin(chatId: Long, value: Boolean): Boolean {
        return db.chatDao.updateIsPinned(ChatPinUpdate(chatId, value)) == 1
    }

    override fun updateHide(chatId: Long, value: Boolean): Boolean {
        return db.chatDao.updateIsHidden(ChatHideUpdate(chatId, value)) == 1
    }

    override fun updateArchive(chatId: Long, value: Boolean): Boolean {
        return db.chatDao.updateArchived(ChatArchiveUpdate(chatId, value)) == 1
    }

    override fun updateTags(chatId: Long, value: List<String>): Boolean {
        return db.chatDao.updateTags(ChatTagsUpdate(chatId, value)) == 1
    }

    override fun updateNotificationEnabled(chatId: Long, value: Boolean): Boolean {
        return db.chatDao.updateNotificationEnabled(ChatNotificationEnabledUpdate(chatId, value)) == 1
    }

    override fun containsContact(contactId: Long): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    withContext(Dispatchers.IO) {
                        db.contactChatCrossRefDao.getChatsByContactId(contactId).let { chatIds ->
                            Resource.Success(db.chatDao.getChatsByIds(chatIds).map { it.toChat() })
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error" + e.message))
            }
        }
    }

    override fun withCustomTag(customTagChatFilter: ChatFilter.Tag): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emitAll(
                    db.chatDao.queryChatByTag(customTagChatFilter.label).map {
                        Resource.Success(it.map { it.toChat() })
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error("unknown error" + e.message))
            }
        }
    }
}