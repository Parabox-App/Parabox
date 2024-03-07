package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.parabox.data.local.entity.ContactChatCrossRef

@Dao
interface ContactChatCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContactChatCrossRef(contactChatCrossRef: ContactChatCrossRef) : Long

    @Query("SELECT chatId FROM contact_chat_cross_ref WHERE contactId = :contactId AND chatId = :chatId LIMIT 1")
    fun checkContactChatCrossRef(contactId: Long, chatId: Long): Long?

    @Query("SELECT chatId FROM contact_chat_cross_ref WHERE contactId = :contactId")
    fun getChatsByContactId(contactId: Long): List<Long>

    @Query("SELECT contactId FROM contact_chat_cross_ref WHERE chatId = :chatId")
    fun getContactsByRootChatId(chatId: Long): List<Long>
}