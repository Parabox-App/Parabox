package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.*
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.PluginConnection
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT EXISTS(SELECT * FROM contact_entity WHERE contactId = :contactId)")
    suspend fun isExist(contactId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Query("DELETE FROM contact_entity WHERE contactId = :contactId")
    suspend fun deleteContact(contactId: Long): Int

    @Update
    fun updateContact(contacts: List<ContactEntity>)

    @Update(entity = ContactEntity::class)
    fun updateHiddenState(obj: ContactHiddenStateUpdate)

    @Update(entity = ContactEntity::class)
    fun updateHiddenState(objList: List<ContactHiddenStateUpdate>)

    @Update(entity = ContactEntity::class)
    fun updateProfileAndTag(obj: ContactProfileAndTagUpdate)

    @Update(entity = ContactEntity::class)
    fun updateTag(obj: ContactTagUpdate)

    @Update(entity = ContactEntity::class)
    fun updatePinnedState(obj: ContactPinnedStateUpdate)

    @Update(entity = ContactEntity::class)
    fun updateNotificationState(obj: ContactNotificationStateUpdate)

    @Update(entity = ContactEntity::class)
    fun updateArchivedState(obj: ContactArchivedStateUpdate)

    @Update(entity = ContactEntity::class)
    fun updateUnreadMessagesNum(obj: ContactUnreadMessagesNumUpdate)

    @Update(entity = ContactEntity::class)
    fun updateShouldBackup(obj: ContactShouldBackupUpdate)

    @Query("SELECT * FROM contact_entity WHERE contactId = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contact_entity WHERE contactId IN (:ids)")
    suspend fun getContactByIds(ids: List<Long>): List<ContactEntity>

    @Query("SELECT * FROM contact_entity")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE content IS NOT NULL")
    fun getMessagedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE isArchived")
    fun getArchivedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE isHidden")
    fun getAllHiddenContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE NOT isHidden AND NOT isArchived")
    fun getAllUnhiddenContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE sender = name")
    suspend fun getPersonalContacts(): List<ContactEntity>

    @Query("SELECT * FROM contact_entity WHERE sender != name ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getGroupContacts(limit: Int): List<ContactEntity>

    @Query("SELECT * FROM contact_entity WHERE name LIKE '%' || :query || '%'")
    fun queryContact(query: String): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPluginConnection(pluginConnection: PluginConnectionEntity): Long

    @Delete
    fun deletePluginConnection(pluginConnection: PluginConnectionEntity)

    @Query("SELECT * FROM plugin_connection_entity WHERE objectId = :objectId")
    fun getPluginConnectionById(objectId: Long): PluginConnectionEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContactPluginConnectionCrossRef(crossRef: ContactPluginConnectionCrossRef)

    @Delete
    fun deleteContactPluginConnectionCrossRef(crossRef: ContactPluginConnectionCrossRef)

    @Query("DELETE FROM contact_plugin_connection_cross_ref WHERE contactId = :contactId")
    suspend fun deleteContactPluginConnectionCrossRefByContactId(contactId: Long): Int

    @Query("SELECT * FROM contact_plugin_connection_cross_ref WHERE contactId = :contactId")
    fun getContactPluginConnectionCrossRefsByContactId(contactId: Long): List<ContactPluginConnectionCrossRef>

    @Query("SELECT * FROM contact_plugin_connection_cross_ref WHERE objectId = :objectId")
    fun getContactPluginConnectionCrossRefsByObjectId(objectId: Long): List<ContactPluginConnectionCrossRef>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId = :contactId LIMIT 1")
    fun getContactWithPluginConnections(contactId: Long): ContactWithPluginConnections

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId IN (:contactIds)")
    fun getContactWithPluginConnectionsByList(contactIds: List<Long>): List<ContactWithPluginConnections>

    @Transaction
    @Query("SELECT * FROM plugin_connection_entity WHERE objectId = :objectId LIMIT 1")
    fun getPluginConnectionWithContacts(objectId: Long): PluginConnectionWithContacts
}