package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.data.local.entity.ChatLatestMessageIdUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertChat(chat: ChatEntity): Long

    @Transaction
    @Query(
        "SELECT chat_entity.* FROM chat_entity " +
                "INNER JOIN message_entity ON chat_entity.latestMessageId = message_entity.messageId " +
                "ORDER BY message_entity.timestamp DESC"
    )
    fun getChatPagingSource(): PagingSource<Int, ChatWithLatestMessageEntity>

    @Query("SELECT * FROM chat_entity WHERE chatId = :id LIMIT 1")
    suspend fun getChatById(id: Long): ChatEntity?

    @Query("SELECT * FROM chat_entity WHERE chatId IN (:ids)")
    suspend fun getChatsByIds(ids: List<Long>): List<ChatEntity>

    @Query("SELECT * FROM chat_entity")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE isArchived")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE isHidden")
    fun getAllHiddenChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE NOT isHidden AND NOT isArchived")
    fun getAllUnhiddenChats(): Flow<List<ChatEntity>>

    @Update(entity = ChatEntity::class)
    fun updateLatestMessageId(obj: ChatLatestMessageIdUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateUnreadMessageNum(obj: ChatUnreadMessagesNumUpdate): Int
}