package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QueryContactAndMessage @Inject constructor(
    val repository: MainRepository
) {
    fun groupedMessage(query: String): Flow<Resource<List<ContactWithMessages>>> {
        return repository.queryContactWithMessages(query)
    }

    fun contact(query: String): Flow<Resource<List<Contact>>> {
        return repository.queryContact(query)
    }
}