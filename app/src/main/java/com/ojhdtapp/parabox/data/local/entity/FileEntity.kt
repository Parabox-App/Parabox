package com.ojhdtapp.parabox.data.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.File
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Entity(tableName = "file_entity")
data class FileEntity(
    val url: String,
    val name: String,
    val extension: String,
    val size: Long,
    val timestamp: Long,
    val profileName: String,
    val downloadingState: DownloadingState = DownloadingState.None,
    val downloadPath: String? = null,
    val relatedMessageId: Long,
    @PrimaryKey(autoGenerate = true) val fileId: Long = 0,
    val downloadId: Long? = null,
) {
    fun toFile() = File(
        url,
        name,
        extension,
        size,
        timestamp,
        profileName,
        downloadingState,
        downloadPath,
        relatedMessageId,
        fileId,
        downloadId
    )
}

@Entity
data class FileDownloadingStateUpdate(
    @ColumnInfo(name = "fileId")
    val fileId: Long,
    @ColumnInfo(name = "downloadingState")
    val downloadingState: DownloadingState,
)

data class FileDownloadInfoUpdate(
    @ColumnInfo(name = "fileId")
    val fileId: Long,
    @ColumnInfo(name = "downloadPath")
    val downloadPath: String?,
    @ColumnInfo(name = "downloadId")
    val downloadId: Long?
)

sealed class DownloadingState {
    data class Downloading(val downloadedBytes: Int, val totalBytes: Int) : DownloadingState() {
        val progress = if (totalBytes == 0) 0f else (downloadedBytes.toFloat() / totalBytes)
    }

    object None : DownloadingState()

    object Failure : DownloadingState()

    object Done : DownloadingState()
}
