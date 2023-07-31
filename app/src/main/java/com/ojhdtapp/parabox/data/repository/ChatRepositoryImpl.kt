package com.ojhdtapp.parabox.data.repository

import android.content.Context
import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatArchiveUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatBeanEntity
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ChatHideUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatPinUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
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

    override fun getPinnedChatPagingSource(): PagingSource<Int, ChatEntity> {
        return db.chatDao.getPinnedChatPagingSource()
    }

    override fun queryChatWithLimit(query: String, limit: Int): Flow<Resource<List<Chat>>> {
        return flow {
            emit(Resource.Loading())
            try {
                if(limit == 0) {
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
}