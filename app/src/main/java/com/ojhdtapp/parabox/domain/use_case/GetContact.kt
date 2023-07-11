package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetContact @Inject constructor(
    val repository: ContactRepository
) {
    fun byId(contactId: Long): Flow<Resource<Contact>> {
        return repository.getContactById(contactId)
    }
}