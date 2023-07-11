package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessage @Inject constructor(
    val repository: MessageRepository
) {
    fun byId(messageId: Long): Flow<Resource<Message>> {
        return repository.getMessageById(messageId)
    }
}