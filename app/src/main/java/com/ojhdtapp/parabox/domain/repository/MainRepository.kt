package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactWithPluginConnections
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.GroupInfoPack
import com.ojhdtapp.parabox.domain.model.PluginConnection
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun handleNewMessage(dto: MessageDto)
    fun getAllHiddenContacts(): Flow<Resource<List<Contact>>>
    fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>>
    fun getPluginConnectionObjectIdListByContactId(contactId: Long): List<Long>
    fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>>
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>>
    fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack?
}