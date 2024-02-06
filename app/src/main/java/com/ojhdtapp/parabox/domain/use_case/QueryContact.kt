package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QueryContact @Inject constructor(
    val repository: ContactRepository
) {
    fun byPlatformInfo(pkg: String, uid: String): Flow<Resource<Contact>> {
        return repository.getContactByPlatformInfo(pkg, uid)
    }
}