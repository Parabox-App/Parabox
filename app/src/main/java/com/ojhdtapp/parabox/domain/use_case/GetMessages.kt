package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.*
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessages @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(contact: Contact): Flow<Resource<ContactWithMessages>> {
        repository.getPluginConnectionByContactId(contactId = contact.contactId).also {
            return repository.getSpecifiedListOfContactWithMessages(it.map { it.objectId })
                .map<Resource<List<ContactWithMessages>>, Resource<ContactWithMessages>> { contactWithMessagesListResource ->
                    return@map when (contactWithMessagesListResource) {
                        is Resource.Error -> Resource.Error(contactWithMessagesListResource.message!!)
                        is Resource.Loading -> Resource.Loading()
                        is Resource.Success -> Resource.Success(
                            ContactWithMessages(
                                contact = contact,
                                messages = contactWithMessagesListResource.data?.fold(initial = mutableListOf<Message>()) { acc, contactWithMessages ->
                                    acc.addAll(contactWithMessages.messages)
                                    acc
                                }?.sortedBy { it.timestamp }?.toList() ?: emptyList<Message>()
                            )
                        )
                    }
                }
        }
    }
    // Must called within coroutine
    fun pluginConnectionList(contact: Contact) : List<PluginConnection>{
        return repository.getPluginConnectionByContactId(contactId = contact.contactId)
    }

    fun pagingFlow(pluginConnectionObjectIdList: List<Long>): Flow<PagingData<Message>> {
            return Pager(
                PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = true,
                )
            ) { repository.getMessagesPagingSource(pluginConnectionObjectIdList) }.flow
                .map { pagingData ->
                    pagingData.map {
                        it.toMessage()
                    }
                }
        }
}