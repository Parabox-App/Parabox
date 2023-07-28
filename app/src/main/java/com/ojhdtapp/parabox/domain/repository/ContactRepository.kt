package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun queryContact(query: String): Flow<Resource<List<Contact>>>
    fun getContactWithLimit(limit: Int): Flow<Resource<List<Contact>>>
    fun getContactById(contactId: Long): Flow<Resource<Contact>>
}