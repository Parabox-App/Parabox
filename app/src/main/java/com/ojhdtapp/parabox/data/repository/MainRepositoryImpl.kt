package com.ojhdtapp.parabox.data.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : MainRepository {
    override suspend fun handleNewMessage(dto: MessageDto) {
        database.messageDao.insertMessage(dto.toMessageEntity())
        database.contactDao.insertContact(dto.toContactEntity())
        database.contactMessageCrossRefDao.insertNewContactMessageCrossRef(dto.getContactMessageCrossRef())
    }

    override fun getAllHiddenContacts(): Flow<Resource<List<Contact>>> {
//        return database.contactDao.getAllHiddenContacts().map { contactEntityList ->
//            contactEntityList.map {
//                it.toContact()
//            }
//        }
        return flow {
            emit(Resource.Loading())
            emitAll(database.contactDao.getAllHiddenContacts().map { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                })
            }.catch {
                Resource.Error<List<Contact>>("获取数据时发生错误")
            })
        }
    }

    override fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading())
            emitAll(database.contactDao.getAllUnhiddenContacts().map { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                })
            }.catch {
                Resource.Error<List<Contact>>("获取数据时发生错误")
            })
        }
    }
}