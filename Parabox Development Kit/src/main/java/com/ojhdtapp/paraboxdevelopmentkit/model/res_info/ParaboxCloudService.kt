package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ParaboxCloudService {
    suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus>
    suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus>
    fun isDownloadMatched(resourceInfo: ParaboxResourceInfo): Boolean
    fun isUploadMatched(resourceInfo: ParaboxResourceInfo): Boolean
}

sealed interface ParaboxCloudStatus {
    data class Waiting(
        val resourceInfo: ParaboxResourceInfo
    ) : ParaboxCloudStatus
    data class Uploading(
        val localResource: ParaboxResourceInfo.ParaboxLocalInfo,
        val progress: Float,
        val total: Long,
        val speed: Long,
    ) : ParaboxCloudStatus
    data class Downloading(
        val remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo,
        val progress: Float,
        val total: Long,
        val speed: Long,
    ) : ParaboxCloudStatus
    data class Synced(
        val localResource: ParaboxResourceInfo.ParaboxLocalInfo,
        val remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo,

    ) : ParaboxCloudStatus
    data object Failed : ParaboxCloudStatus
}