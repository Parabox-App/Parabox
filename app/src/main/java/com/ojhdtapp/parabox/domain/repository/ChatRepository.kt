package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatPagingSource(filter: List<ChatFilter>) : PagingSource<Int, ChatWithLatestMessageEntity>
    fun getPinnedChatPagingSource() : PagingSource<Int, ChatEntity>
    fun getChatById(id: Long) : Flow<Resource<Chat>>
    fun queryChatWithLimit(query: String, limit: Int): Flow<Resource<List<Chat>>>
    fun getChatWithLimit(limit: Int): Flow<Resource<List<Chat>>>
    fun getNotificationDisabledChat(): Flow<Resource<List<Chat>>>
    fun updateUnreadMessagesNum(chatId: Long, value: Int): Boolean
    fun updatePin(chatId: Long, value: Boolean): Boolean
    fun updateHide(chatId: Long, value: Boolean): Boolean
    fun updateArchive(chatId: Long, value: Boolean): Boolean
    fun updateTags(chatId: Long, value: List<String>): Boolean
    fun updateNotificationEnabled(chatId: Long, value: Boolean): Boolean
    fun containsContact(contactId: Long) : Flow<Resource<List<Chat>>>
    fun withCustomTag(customTagChatFilter: ChatFilter.Tag) : Flow<Resource<List<Chat>>>
}