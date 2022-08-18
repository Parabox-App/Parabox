package com.ojhdtapp.parabox.domain.use_case

import android.util.Log
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactPluginConnectionCrossRef
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class DeleteGroupedContact @Inject constructor(
    val repository: MainRepository
) {
    var lastDelete: Pair<ContactEntity?, List<ContactPluginConnectionCrossRef>>? = null
    suspend operator fun invoke(contact: Contact?, contactId: Long? = null) {
        if (contactId != null ||
            (contact != null && contact.senderId != contact.contactId)
        ) {
            lastDelete = repository.deleteGroupedContact(contactId ?: contact!!.contactId)
        }
    }

    suspend fun revoke() {
        lastDelete?.let {
            if (it.first != null) {
                repository.restoreGroupedContact(it.first!! to it.second)
            }
        }
    }
}