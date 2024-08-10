package com.ojhdtapp.parabox.data.local

import android.os.Bundle
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoExtraUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertConnectionInfo(connectionInfo: ConnectionInfoEntity): Long

    @Query("DELETE FROM connection_info_entity WHERE connectionId = :connectionId")
    fun deleteConnectionInfoById(connectionId: Long): Int

    @Query("DELETE FROM connection_info_entity WHERE pkg = :pkg")
    fun deleteConnectionInfoByPackageName(pkg: String): Int

    @Query("SELECT * FROM connection_info_entity")
    fun getConnectionInfoList() : Flow<List<ConnectionInfoEntity>>

    @Update(entity = ConnectionInfoEntity::class)
    fun updateConnectionInfoExtra(obj: ConnectionInfoExtraUpdate): Int
}