package com.ojhdtapp.parabox.domain.cloud

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCustomCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ParaboxCustomCloudServiceWrapper(val customCloudService: ParaboxCustomCloudService, val key: String) : ParaboxCloudService {
    override suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus> {
        return MutableStateFlow(ParaboxCloudStatus.Failed)
    }

    override suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus> {
        if (isDownloadMatched(remoteResource)) {
            return customCloudService.download(remoteResource as ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo)
        } else {
            return MutableStateFlow(ParaboxCloudStatus.Failed)
        }
    }

    override fun isDownloadMatched(resourceInfo: ParaboxResourceInfo): Boolean {
        return resourceInfo is ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo && resourceInfo.key == key
    }

    override fun isUploadMatched(resourceInfo: ParaboxResourceInfo): Boolean {
        return false
    }
}