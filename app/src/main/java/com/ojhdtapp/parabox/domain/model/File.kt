package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.data.local.entity.FileEntity

data class File(
    val url: String?,
    val name: String,
    val extension: String,
    val size: Long,
    val timestamp: Long,
    val profileName: String,
    val downloadingState: DownloadingState = DownloadingState.None,
    val downloadPath: String? = null,
    val relatedContactId: Long? = null,
    val relatedMessageId: Long? = null,
    val fileId : Long,
    val downloadId: Long? = null,
    val cloudType: Int? = null,
    val cloudId: String? = null,
) {
    fun toFileEntity() = FileEntity(
        url,
        name,
        extension,
        size,
        timestamp,
        profileName,
        downloadingState,
        downloadPath,
        relatedContactId,
        relatedMessageId,
        fileId,
        downloadId,
        cloudType,
        cloudId
    )
}