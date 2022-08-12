package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import coil.request.Tags
import com.ojhdtapp.messagedto.MessageDto
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import com.ojhdtapp.parabox.data.local.entity.ContactWithPluginConnections
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun handleNewMessage(dto: MessageDto)
    fun updateContactHiddenState(id: Long, value: Boolean)
    fun updateContactPinnedState(id: Long, value: Boolean)
    fun updateContactNotificationState(id: Long, value: Boolean)
    fun updateContactArchivedState(id: Long, value: Boolean)
    fun updateContactTag(id: Long, tag: List<String>)
    fun updateContactProfileAndTag(id: Long, profile: Profile, tags: List<String>)
    fun updateContactUnreadMessagesNum(id:Long, value: Int)
    fun getContactTags(): Flow<List<Tag>>
    fun deleteContactTag(value: String)
    fun addContactTag(value: String)
    fun checkContactTag(value: String): Boolean
    fun getAllHiddenContacts(): Flow<Resource<List<Contact>>>
    fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>>
    fun getArchivedContacts() : Flow<Resource<List<Contact>>>
    fun getPluginConnectionObjectIdListByContactId(contactId: Long): List<Long>
    fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>>
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>>
    fun getMessagesPagingSource(contactIds: List<Long>) : PagingSource<Int, MessageEntity>
    fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack?
    suspend fun groupNewContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long
    ): Boolean
}