package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUngroupedMessages @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(contact: Contact): Flow<Resource<ContactWithMessages>> {
        return repository.getSpecifiedContactWithMessages(contact.connections.first().objectId)
    }
}