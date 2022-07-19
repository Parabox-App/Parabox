package com.ojhdtapp.parabox.domain.repository

import coil.request.Tags
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactWithPluginConnections
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun handleNewMessage(dto: MessageDto)
    fun updateContactHiddenState(id: Long, value: Boolean)
    fun updateContactProfileAndTag(id: Long, profile: Profile, tags: List<String>)
    fun getContactTags(): Flow<List<Tag>>
    fun deleteContactTag(value: String)
    fun addContactTag(value: String)
    fun getAllHiddenContacts(): Flow<Resource<List<Contact>>>
    fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>>
    fun getPluginConnectionObjectIdListByContactId(contactId: Long): List<Long>
    fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>>
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>>
    fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack?
    suspend fun groupNewContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long
    ): Boolean
}