package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Query @Inject constructor(
    val messageRepository: MessageRepository,
    val contactRepository: ContactRepository,
    val chatRepository: ChatRepository
) {
    fun message(query: String): Flow<Resource<List<QueryMessage>>> {
        return messageRepository.queryMessage(query)
    }
}