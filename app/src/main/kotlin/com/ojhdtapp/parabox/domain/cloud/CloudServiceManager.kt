package com.ojhdtapp.parabox.domain.cloud

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.ojhdtapp.parabox.core.util.awaitUntilSuccess
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentSkipListSet

class CloudServiceManager(
    context: Context,
    val ktorCloudService: KtorCloudServiceImpl
) : ParaboxCloudService {
    private val customCloudService = mutableSetOf<ParaboxCloudService>()

    private val runningDownloadTask = ConcurrentSkipListSet<ParaboxResourceInfo.ParaboxRemoteInfo>()

    fun registerCloudService(cloudService: ParaboxCloudService): Boolean {
        Log.d(TAG, "registerCloudService: $cloudService")
        if (customCloudService.contains(cloudService)) {
            return false
        } else {
            customCloudService.add(cloudService)
            return true
        }
    }

    suspend fun fastDownload(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo, timeoutMills: Long): ParaboxResourceInfo.ParaboxSyncedInfo? {
        return download(remoteResource).awaitUntilSuccess(timeoutMills)?.let { ParaboxResourceInfo.ParaboxSyncedInfo(it.localResource, it.remoteResource) } ?: run {
            runningDownloadTask.remove(remoteResource)
            null
        }
    }

    override suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus> {
        if (ktorCloudService.isUploadMatched(localResource)) {
            return ktorCloudService.upload(localResource)
        }
        return MutableStateFlow(ParaboxCloudStatus.Failed)
    }

    override suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus> {
        if (runningDownloadTask.contains(remoteResource)) {
            return MutableStateFlow(ParaboxCloudStatus.Failed)
        } else {
            runningDownloadTask.add(remoteResource)
        }
        if (remoteResource is ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo) {
            customCloudService.forEach {
                if (it.isDownloadMatched(remoteResource)) {
                    return it.download(remoteResource).onEach {
                        if (it is ParaboxCloudStatus.Synced || it is ParaboxCloudStatus.Failed) {
                            runningDownloadTask.remove(remoteResource)
                        }
                    }
                }
            }
        }
        if (ktorCloudService.isDownloadMatched(remoteResource)) {
            return ktorCloudService.download(remoteResource).onEach {
                if (it is ParaboxCloudStatus.Synced) {
                    runningDownloadTask.remove(remoteResource)
                }
            }
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

        const val TIMEOUT_MILLS = 5000L
    }
}

val LocalCloudService = staticCompositionLocalOf<CloudServiceManager> {
    error("No cloud service provided")
}