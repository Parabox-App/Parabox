package com.ojhdtapp.parabox.data.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

class ContactRepositoryImpl : ContactRepository {
    override fun queryContact(query: String): Flow<Resource<List<Contact>>> {
        TODO("Not yet implemented")
    }
}