package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import kotlinx.coroutines.flow.Flow

interface ParaboxCustomCloudService {
    suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo): Flow<ParaboxCloudStatus>
}