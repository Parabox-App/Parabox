package com.ojhdtapp.parabox.data.repository

import android.util.Log
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.*
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.*
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : MainRepository {
    override suspend fun handleNewMessage(dto: MessageDto) {
        coroutineScope {
            val messageIdDeferred = async<Long> {
                database.messageDao.insertMessage(dto.toMessageEntity())
            }
            val contactIdDeferred = async<Long> {
                database.contactDao.insertContact(
//                    dto.toContactEntityWithUnreadMessagesNumUpdate(
//                        database.contactDao
//                    )
                    dto.toContactEntity()
                )
            }
            val pluginConnectionDeferred = async<Long> {
                database.contactDao.insertPluginConnection(dto.pluginConnection.toPluginConnectionEntity())
            }
//            database.contactDao.updateHiddenState(ContactHiddenStateUpdate(dto.pluginConnection.objectId, false))
            if (pluginConnectionDeferred.await() != -1L) {
                database.contactDao.insertContactPluginConnectionCrossRef(
                    ContactPluginConnectionCrossRef(
                        contactId = dto.pluginConnection.objectId,
                        objectId = pluginConnectionDeferred.await()
                    )
                )
            }
            database.contactMessageCrossRefDao.insertContactMessageCrossRef(
                ContactMessageCrossRef(
                    contactId = dto.pluginConnection.objectId,
                    messageId = messageIdDeferred.await()
                )
            )
//            database.contactDao.getContactPluginConnectionCrossRefsByObjectId(dto.pluginConnection.objectId)
//                .map {
//                    ContactHiddenStateUpdate(contactId = it.contactId, isHidden = false)
//                }.let {
//                    database.contactDao.updateHiddenState(it)
//                }


            // Update Avatar or Anything Else Here
            database.contactDao.getPluginConnectionWithContacts(dto.pluginConnection.objectId).let {
                database.contactDao.updateContact(it.contactList.map {
                    it.copy(
                        latestMessage = LatestMessage(
                            content = dto.contents.getContentString(),
                            timestamp = dto.timestamp,
                            unreadMessagesNum = (it.latestMessage?.unreadMessagesNum ?: 0) + 1
                        ),
                        isHidden = false
                    )
                })
            }
        }
//        database.messageDao.insertMessage(dto.toMessageEntity())
//        database.contactDao.insertContact(dto.toContactEntityWithUnreadMessagesNumUpdate(database.contactDao))
//        database.contactMessageCrossRefDao.insertNewContactMessageCrossRef(dto.getContactMessageCrossRef())
    }

    override fun updateContactHiddenState(id: Long, value: Boolean) {
        database.contactDao.updateHiddenState(ContactHiddenStateUpdate(id, value))
    }

    override fun getAllHiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllHiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 })
            }.catch() {
                emit(Resource.Error<List<Contact>>("获取数据时发生错误"))
            }
    }

    override fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllUnhiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 })
            }.catch {
                emit(Resource.Error<List<Contact>>("获取数据时发生错误"))
            }
    }

    override fun getPluginConnectionObjectIdListByContactId(contactId: Long): List<Long> {
        return database.contactDao.getContactPluginConnectionCrossRefsByContactId(contactId = contactId)
            .map {
                it.objectId
            }
    }

    @OptIn(FlowPreview::class)
    override fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>> {
        return flow<Resource<ContactWithMessages>> {
            emit(Resource.Loading())
        }.flatMapConcat {
            database.contactMessageCrossRefDao.getSpecifiedContactWithMessages(contactId)
                .map<ContactWithMessagesEntity, Resource<ContactWithMessages>> {
                    Resource.Success(it.toContactWithMessages())
                }.catch {
                    emit(Resource.Error<ContactWithMessages>("获取数据时发生错误"))
                }
        }
//        return database.contactMessageCrossRefDao.getSpecifiedContactWithMessages(contactId)
//            .map<List<ContactWithMessagesEntity>, Resource<List<ContactWithMessages>>> { contactWithMessagesEntityList ->
//                Resource.Success(contactWithMessagesEntityList.map {
//                    it.toContactWithMessages()
//                })
//            }.catch {
//                emit(Resource.Error<List<ContactWithMessages>>("获取数据时发生错误"))
//            }
    }

    @OptIn(FlowPreview::class)
    override fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>> {
        return flow<Resource<List<ContactWithMessages>>> {
            emit(Resource.Loading())
        }.flatMapConcat {
            database.contactMessageCrossRefDao.getSpecifiedListOfContactWithMessages(contactIds)
                .map<List<ContactWithMessagesEntity>, Resource<List<ContactWithMessages>>> { contactWithMessagesEntityList ->
                    Resource.Success(contactWithMessagesEntityList.map {
                        it.toContactWithMessages()
                    })
                }.catch {
                    emit(Resource.Error<List<ContactWithMessages>>("获取数据时发生错误"))
                }
        }
    }

    override fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack? {
        val contactList = mutableListOf<Contact>()
        val pluginConnectionList = mutableListOf<PluginConnection>()
        database.contactDao.getContactWithPluginConnectionsByList(contactIds).forEach {
            contactList.add(it.contact.toContact())
            pluginConnectionList.addAll(it.pluginConnectionList.map { it.toPluginConnection() })
        }
        return if (contactList.isEmpty() || pluginConnectionList.isEmpty()) null else GroupInfoPack(
            contacts = contactList,
            pluginConnectionsDistinct = pluginConnectionList.distinct()
        )
    }

    override suspend fun groupNewContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long
    ): Boolean {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    val contactEntity = ContactEntity(
                        profile = Profile(
                            name = name,
                            avatar = null,
                        ),
                        latestMessage = null,
                        senderId = senderId
                    )
                    val contactId = database.contactDao.insertContact(contactEntity)
                    pluginConnections.forEach { conn ->
                        database.contactDao.insertContactPluginConnectionCrossRef(
                            ContactPluginConnectionCrossRef(
                                contactId = contactId,
                                objectId = conn.objectId
                            )
                        )
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

            }
        }
    }
}