package com.ojhdtapp.parabox.data.repository

import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    val database: AppDatabase
) : MainRepository {
    override suspend fun receiveNewMessage() {
        val message = Message(
            listOf(
                PlainText("Hello")
            ),
            MessageProfile("Ojhdt", null),
            System.currentTimeMillis()
        )
        database.messageDao.insertMessage(message.toMessageEntity(1))
        database.contactDao.insertContact(
            Contact(
                "Ojhdt",
                null, message.contents.getContentString(), PluginConnection(0, 0)
            ).toContactEntity(1)
        )
    }
}

//"$acc ${messageContent.getContentString()}"