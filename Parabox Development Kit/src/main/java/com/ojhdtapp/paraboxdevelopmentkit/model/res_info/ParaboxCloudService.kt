package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ParaboxCloudService {
    suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus>
    suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus>
}

sealed interface ParaboxCloudStatus {
    data class Waiting(
        val resourceInfo: ParaboxResourceInfo
    ) : ParaboxCloudStatus
    data class Uploading(
        val localUri: Uri,
        val progress: Float,
        val total: Long,
        val speed: Long,
    ) : ParaboxCloudStatus
    data class Downloading(
        val remotePath: String,
        val progress: Float,
        val total: Long,
        val speed: Long,
    ) : ParaboxCloudStatus
    data class Synced(
        val localUri: Uri,
        val remoteUrl: String,

    ) : ParaboxCloudStatus
    data object Failed : ParaboxCloudStatus
}