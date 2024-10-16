package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Query @Inject constructor(
    private val mainRepository: MainRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
) {
    companion object {
        const val SEARCH_RECENT_DATA_NUM = 6
    }

    fun recentQuery(): Flow<Resource<List<RecentQuery>>> {
        return mainRepository.getRecentQuery()
    }

    suspend fun submitRecentQuery(value: String): Boolean {
        return mainRepository.submitRecentQuery(value)
    }

    suspend fun deleteRecentQuery(id: Long): Boolean {
        return mainRepository.deleteRecentQuery(id)
    }

    fun recentMessage(): Flow<Resource<List<QueryMessage>>> {
        return messageRepository.getMessageWithLimit(SEARCH_RECENT_DATA_NUM)
    }

    fun message(query: String, withLimit: Boolean): Flow<Resource<List<QueryMessage>>> {
        return messageRepository.queryMessageWithLimit(query, if(withLimit) SEARCH_RECENT_DATA_NUM else 0)
    }

    fun recentContact(): Flow<Resource<List<Contact>>> {
        return contactRepository.getContactWithLimit(SEARCH_RECENT_DATA_NUM)
    }

    fun contact(query: String, withLimit: Boolean): Flow<Resource<List<Contact>>> {
        return contactRepository.queryContactWithLimit(query, if (withLimit) SEARCH_RECENT_DATA_NUM else 0)
    }

    fun recentChat(): Flow<Resource<List<Chat>>> {
        return chatRepository.getChatWithLimit(SEARCH_RECENT_DATA_NUM)
    }

    fun chat(query: String, withLimit: Boolean): Flow<Resource<List<Chat>>> {
        return chatRepository.queryChatWithLimit(query, if (withLimit) SEARCH_RECENT_DATA_NUM else 0)
    }
}