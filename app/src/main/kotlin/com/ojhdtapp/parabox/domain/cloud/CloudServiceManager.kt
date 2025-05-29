package com.ojhdtapp.parabox.domain.cloud

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class CloudServiceManager(
    context: Context,
    val ktorCloudService: KtorCloudServiceImpl
) : ParaboxCloudService {
    private val customCloudService = mutableSetOf<ParaboxCloudService>()

    fun registerCloudService(cloudService: ParaboxCloudService): Boolean {
        Log.d(TAG, "registerCloudService: $cloudService")
        if (customCloudService.contains(cloudService)) {
            return false
        } else {
            customCloudService.add(cloudService)
            return true
        }
    }

    override suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus> {
        if (ktorCloudService.isUploadMatched(localResource)) {
            return ktorCloudService.upload(localResource)
        }
        return MutableStateFlow(ParaboxCloudStatus.Failed)
    }

    override suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus> {
        if (remoteResource is ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo) {
            customCloudService.forEach {
                if (it.isDownloadMatched(remoteResource)) {
                    return it.download(remoteResource)
                }
            }
        }
        if (ktorCloudService.isDownloadMatched(remoteResource)) {
            return ktorCloudService.download(remoteResource)
        }
        return MutableStateFlow(ParaboxCloudStatus.Failed)
    }

    override fun isDownloadMatched(resourceInfo: ParaboxResourceInfo): Boolean {
        return true
    }

    override fun isUploadMatched(resourceInfo: ParaboxResourceInfo): Boolean {
        return true
    }

    companion object {
        const val TAG = "CloudServiceManager"
    }
}

val LocalCloudService = staticCompositionLocalOf<ParaboxCloudService> {
    error("No cloud service provided")
}