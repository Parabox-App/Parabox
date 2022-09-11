package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.data.local.entity.FileEntity

data class File(
    val url: String,
    val name: String,
    val extension: String,
    val size: Long,
    val timestamp: Long,
    val profileName: String,
    val downloadingState: DownloadingState = DownloadingState.None,
    val downloadPath: String? = null,
    val relatedMessageId: Long,
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
        relatedMessageId
    )
}