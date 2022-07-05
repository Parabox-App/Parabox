package com.ojhdtapp.parabox.data.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
                database.contactDao.insertContact(dto.toContactEntityWithUnreadMessagesNumUpdate(database.contactDao))
            }
            database.contactMessageCrossRefDao.insertNewContactMessageCrossRef(
                ContactMessageCrossRef(contactId = contactIdDeferred.await(), messageId = messageIdDeferred.await())
            )
        }
//        database.messageDao.insertMessage(dto.toMessageEntity())
//        database.contactDao.insertContact(dto.toContactEntityWithUnreadMessagesNumUpdate(database.contactDao))
//        database.contactMessageCrossRefDao.insertNewContactMessageCrossRef(dto.getContactMessageCrossRef())
    }

    override fun getAllHiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllHiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp?:0 })
            }.catch {
                emit(Resource.Error<List<Contact>>("获取数据时发生错误"))
            }
    }

    override fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllUnhiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp?:0 })
            }.catch {
                emit(Resource.Error<List<Contact>>("获取数据时发生错误"))
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
}