package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Query @Inject constructor(
    val messageRepository: MessageRepository,
    val contactRepository: ContactRepository,
    val chatRepository: ChatRepository,
) {
    fun message(query: String): Flow<Resource<List<QueryMessage>>> {
        return messageRepository.queryMessage(query)
    }

    fun contact(query: String): Flow<Resource<List<Contact>>> {
        return contactRepository.queryContact(query)
    }

    fun chat(query: String): Flow<Resource<List<Chat>>> {
        return chatRepository.queryChat(query)
    }
}