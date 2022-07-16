package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.*
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.PluginConnection
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update(entity = ContactEntity::class)
    fun updateHiddenState(obj: ContactHiddenStateUpdate)

    @Update(entity = ContactEntity::class)
    fun updateHiddenState(objList: List<ContactHiddenStateUpdate>)

    @Query("SELECT * FROM contact_entity WHERE contactId = :id LIMIT 1")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contact_entity WHERE contactId IN (:ids)")
    suspend fun getContactByIds(ids: List<Long>): List<ContactEntity>

    @Query("SELECT * FROM contact_entity")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE content IS NOT NULL")
    fun getMessagedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE isHidden")
    fun getAllHiddenContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE NOT isHidden")
    fun getAllUnhiddenContacts(): Flow<List<ContactEntity>>

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

    @Query("SELECT * FROM contact_plugin_connection_cross_ref WHERE contactId = :contactId")
    fun getContactPluginConnectionCrossRefsByContactId(contactId: Long): List<ContactPluginConnectionCrossRef>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId = :contactId")
    fun getContactWithPluginConnections(contactId: Long): List<ContactWithPluginConnections>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId IN (:contactIds)")
    fun getContactWithPluginConnectionsByList(contactIds: List<Long>): List<ContactWithPluginConnections>

    @Transaction
    @Query("SELECT * FROM plugin_connection_entity WHERE objectId = :objectId")
    suspend fun getPluginConnectionWithContacts(objectId: Long): List<PluginConnectionWithContacts>
}