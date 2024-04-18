package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.ContactBasicInfoUpdate
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactWithExtensionInfoEntity

@Dao
interface ContactDao {
    @Query("SELECT contactId FROM contact_entity " +
            "WHERE pkg = :pkg AND uid = :uid LIMIT 1")
    fun checkContact(pkg: String, uid: String) : Long?

    @Query("SELECT * FROM contact_entity " +
            "WHERE pkg = :pkg AND uid = :uid LIMIT 1")
    fun getContactByPlatformInfo(pkg: String, uid: String) : ContactEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContact(contact: ContactEntity): Long

    @Query(
        "SELECT * FROM contact_entity " +
                "WHERE contactId = :contactId " +
                "LIMIT 1"
    )
    fun getContactById(contactId: Long): ContactEntity?

    @Query("SELECT * FROM contact_entity WHERE name LIKE '%' || :query || '%'")
    fun queryContact(query: String): List<ContactEntity>

    @Query("SELECT * FROM contact_entity WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    fun queryContactWithLimit(query: String, limit: Int): List<ContactEntity>

    @Query(
        "SELECT * FROM contact_entity " +
                "LIMIT :limit"
    )
    fun getContactWithLimit(limit: Int): List<ContactEntity>

    @Update(entity = ContactEntity::class)
    fun updateBasicInfo(obj: ContactBasicInfoUpdate): Int

    @Query("SELECT * FROM contact_entity " +
            "ORDER BY name")
    fun getContactPagingSource(): PagingSource<Int, ContactEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity " +
            "ORDER BY name collate localized")
    fun getContactWithExtensionInfoPagingSource(): PagingSource<Int, ContactWithExtensionInfoEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE isFriend " +
            "ORDER BY name collate localized")
    fun getFriendWithExtensionInfoPagingSource(): PagingSource<Int, ContactWithExtensionInfoEntity>

    @Transaction
    @Query("SELECT contact_entity.* FROM contact_entity " +
            "INNER JOIN contact_chat_cross_ref ref ON contact_entity.contactId == ref.contactId " +
            "WHERE ref.chatId IN (:chatIds) " +
            "ORDER BY contact_entity.name collate localized")
    fun getContactInChatWithExtensionInfoPagingSource(chatIds: List<Long>) : PagingSource<Int, ContactWithExtensionInfoEntity>
}