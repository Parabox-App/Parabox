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
        relatedMessageId
    )
}

@Entity
data class FileDownloadingStateUpdate(
    @ColumnInfo(name = "fileId")
    val fileId: Long,
    @ColumnInfo(name = "downloadingState")
    val downloadingState: DownloadingState,
    @ColumnInfo(name = "downloadPath")
    val downloadPath: String,
)

@Parcelize
sealed class DownloadingState : Parcelable {
    data class Downloading(val downloadedBytes: Int, val totalBytes: Int) : DownloadingState() {
        @IgnoredOnParcel
        val progress = if (totalBytes == 0) 0 else ((downloadedBytes * 100) / totalBytes)
    }

    object None : DownloadingState()

    object Failure : DownloadingState()
}
