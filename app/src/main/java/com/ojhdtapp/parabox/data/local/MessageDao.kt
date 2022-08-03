package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.Message

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM message_entity " +
    "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
    "WHERE contact_message_cross_ref.contactId IN (:contactIds) " + "ORDER BY message_entity.timestamp")
    @RewriteQueriesToDropUnusedColumns
    fun getMessagesPagingSource(contactIds: List<Long>): PagingSource<Int, MessageEntity>


    @Query("SELECT * FROM message_entity " +
            "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
            "WHERE contact_message_cross_ref.contactId IN (:contactIds) " +
            "ORDER BY message_entity.timestamp")
    @RewriteQueriesToDropUnusedColumns
    fun getMessagesPagingSourceTest(contactIds: List<Long>): List<MessageEntity>
}