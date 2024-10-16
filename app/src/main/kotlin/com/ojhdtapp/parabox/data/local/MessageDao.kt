package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.MessageVerifyStateUpdate
import com.ojhdtapp.parabox.data.local.entity.MessageWithSenderEntity
import com.ojhdtapp.parabox.data.local.entity.QueryMessageEntity
import com.ojhdtapp.parabox.domain.model.QueryMessage

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @RawQuery(observedEntities = [MessageEntity::class, ContactEntity::class])
    fun getMessagePagingSource(query: SupportSQLiteQuery): PagingSource<Int, MessageWithSenderEntity>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM message_entity " +
                "WHERE chatId IN (:chatIdList) " + "ORDER BY message_entity.timestamp DESC"
    )
    fun getMessagePagingSource(chatIdList: List<Long>): PagingSource<Int, MessageEntity>

    @Query(
        "SELECT * FROM message_entity " +
                "WHERE messageId = :messageId " +
                "LIMIT 1"
    )
    fun getMessageById(messageId: Long): MessageEntity?

    @Query("DELETE FROM message_entity WHERE messageId = :messageId")
    fun deleteMessageById(messageId: Long): Int

    @Query("DELETE FROM message_entity WHERE messageId IN (:messageIdList)")
    fun deleteMessageById(messageIdList: List<Long>): Int

    @Update(entity = MessageEntity::class)
    fun updateVerifiedState(obj: MessageVerifyStateUpdate): Int

//    @Query("SELECT * FROM message_entity WHERE name LIKE '%' || :query || '%' OR contentString LIKE '%' || :query || '%'")
//    fun queryMessage(query: String): List<MessageEntity>
//
//    @Query("SELECT * FROM message_entity " +
//            "JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
//            "JOIN contact_entity ON contact_entity.contactId = contact_message_cross_ref.contactId " +
//            "WHERE message_entity.name LIKE '%' || :query || '%' OR message_entity.contentString LIKE '%' || :query || '%' ")
//    fun queryContactWithMessages(query: String) : Map<ContactEntity, List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM message_entity WHERE contentString LIKE '%' || :query || '%'")
    fun queryMessage(query: String): List<QueryMessageEntity>

    @Transaction
    @Query("SELECT * FROM message_entity WHERE contentString LIKE '%' || :query || '%' LIMIT :limit")
    fun queryMessageWithLimit(query: String, limit: Int): List<QueryMessageEntity>

    @Transaction
    @Query(
        "SELECT * FROM message_entity " +
                "ORDER BY message_entity.timestamp DESC " +
                "LIMIT :limit"
    )
    fun getMessageWithLimit(limit: Int): List<QueryMessageEntity>
}