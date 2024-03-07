package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertExtensionInfo(extensionInfo: ExtensionInfoEntity): Long

    @Query("DELETE FROM extension_info_entity WHERE extensionId = :extensionId")
    fun deleteExtensionInfoById(extensionId: Long): Int

    @Query("SELECT * FROM extension_info_entity LIMIT 1")
    fun getExtensionInfoList() : Flow<List<ExtensionInfoEntity>>
}