package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.repository.ContactRepository
import javax.inject.Inject

class UpdateContact @Inject constructor(
    private val contactRepository: ContactRepository
) {
    suspend fun syncAvatarResource(contactId: Long): Boolean {
        return contactRepository.syncAvatarResource(contactId)
    }
}