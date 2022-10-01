package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContacts @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(): Flow<Resource<List<Contact>>> {
        return repository.getAllUnhiddenContacts()
    }

    fun personal() : Flow<Resource<List<Contact>>>{
        return repository.getPersonalContacts()
    }

    suspend fun queryById(contactId: Long) : Contact?{
        return repository.getContactById(contactId)
    }
}