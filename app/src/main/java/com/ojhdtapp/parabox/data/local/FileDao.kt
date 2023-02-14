package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.FileAndMessage
import com.ojhdtapp.parabox.data.local.entity.FileCloudInfoUpdate
import com.ojhdtapp.parabox.data.local.entity.FileDownloadInfoUpdate
import com.ojhdtapp.parabox.data.local.entity.FileDownloadingStateUpdate
import com.ojhdtapp.parabox.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFile(file: FileEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertFiles(files: List<FileEntity>)

    @Update(entity = FileEntity::class)
    fun updateDownloadingState(obj: FileDownloadingStateUpdate)
    @Update(entity = FileEntity::class)
    fun updateDownloadInfo(obj: FileDownloadInfoUpdate)
    @Update(entity = FileEntity::class)
    fun updateCloudInfo(obj: FileCloudInfoUpdate)
    @Query("DELETE FROM file_entity WHERE fileId = :fileId")
    suspend fun deleteFileByFileId(fileId: Long)

    @Query("DELETE FROM file_entity WHERE relatedMessageId = :messageId")
    fun deleteFileByMessageId(messageId: Long)
    @Query("SELECT * FROM file_entity")
    fun getAllFiles(): Flow<List<FileEntity>>
    @Query("SELECT * FROM file_entity")
    fun getAllFilesStatic() : List<FileEntity>
    @Query("SELECT * FROM file_entity WHERE name LIKE '%' || :query || '%' OR profilename LIKE '%' || :query || '%'")
    fun queryFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM file_entity WHERE extension in (:extension)")
    fun getFilesByExtensions(extension:List<String>): Flow<List<FileEntity>>

    @Query("SELECT * FROM file_entity WHERE relatedContactId IN (:contactIds)")
    suspend fun getFilesByContactIdsStatic(contactIds: List<Long>): List<FileEntity>
    @Query("SELECT * FROM file_entity WHERE relatedContactId IN (:contactIds) AND cloudType IS NULL AND cloudId IS NULL")
    suspend fun getShouldBackupFiles(contactIds: List<Long>) : List<FileEntity>

    @Transaction
    @Query("SELECT * FROM file_entity WHERE fileId = :fileId LIMIT 1")
    fun getFileAndMessageById(fileId: Long) : FileAndMessage?

    @Transaction
    @Query("SELECT * FROM file_entity WHERE relatedMessageId = :messageId LIMIT 1")
    fun getFileAndMessageByMessageId(messageId: Long) : FileAndMessage?

    @Transaction
    @Query("SELECT * FROM file_entity")
    fun getFilesAndMessages(): Flow<List<FileAndMessage>>
}