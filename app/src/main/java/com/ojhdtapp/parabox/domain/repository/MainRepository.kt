package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import coil.request.Tags
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.*
import com.ojhdtapp.parabox.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun handleNewMessage(dto: ReceiveMessageDto)
    suspend fun handleNewMessage(contents: List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>, pluginConnection: com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection, timestamp: Long, sendType: Int) : Long
    suspend fun deleteGroupedContact(contactId: Long): Pair<ContactEntity?, List<ContactPluginConnectionCrossRef>>
    suspend fun restoreGroupedContact(pair: Pair<ContactEntity, List<ContactPluginConnectionCrossRef>>)
    fun updateMessageVerifiedState(id: Long, value: Boolean)
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
    suspend fun getContactById(contactId: Long): Contact?
    fun getAllHiddenContacts(): Flow<Resource<List<Contact>>>
    fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>>
    fun getArchivedContacts() : Flow<Resource<List<Contact>>>
    fun getPersonalContacts() : Flow<Resource<List<Contact>>>
    fun getGroupContacts(limit: Int): Flow<Resource<List<Contact>>>
    fun getPluginConnectionByContactId(contactId: Long): List<PluginConnection>
    fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>>
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>>
    fun getMessagesPagingSource(contactIds: List<Long>) : PagingSource<Int, MessageEntity>
    fun deleteMessageById(messageId: Long)
    fun deleteMessageById(messageIdList: List<Long>)
    fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack?
    suspend fun groupNewContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long,
        avatar: String? = null,
        avatarUri: String? = null,
        tags: List<String>,
        contactId: Long? = null
    ): Boolean

    fun getFiles(query: String = "") : Flow<Resource<List<File>>>
    fun getAllFilesStatic() : List<File>
    fun updateDownloadingState(state: DownloadingState, target: File)
    fun updateDownloadInfo(path: String?, downloadId: Long?, target: File)
    suspend fun deleteFile(fileId : Long)
}