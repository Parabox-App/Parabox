package com.ojhdtapp.parabox.data.remote.dto.server.content

import android.content.Context
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.FileUtil.toSafeFilename
import com.ojhdtapp.parabox.core.util.TencentCOSUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.use_case.GetUriFromCloudResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed interface Content {
    val type: Int
    fun toMessageContent() : MessageContent
    suspend fun toDownloadedMessageContent(getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo) : MessageContent
}

suspend fun List<Content>.toMessageContentList(
    getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo,
                                       shouldDownloadCloudResource: Boolean = false,
                                       shouldIncludeFile: Boolean = false) : List<MessageContent> {
    return this.map {
        if(shouldDownloadCloudResource){
            if(it.type == 4 && !shouldIncludeFile){
                it.toMessageContent()
            }else{
                it.toDownloadedMessageContent(getUriFromCloudResourceInfo)
            }
        }else{
            it.toMessageContent()
        }
    }
}

data class Text(
    val text: String,
    override val type: Int = 0
) : Content {
    override fun toMessageContent(): MessageContent {
        return PlainText(text)
    }

    override suspend fun toDownloadedMessageContent(getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo): MessageContent {
        return PlainText(text)
    }
}

data class Image(
    val url: String,
    val cloud_type: Int,
    val cloud_id: String,
    val file_name: String,
    override val type: Int = 1
) : Content {
    override fun toMessageContent(): MessageContent {
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image(
            url = url,
            width = 0,
            height = 0,
            fileName = file_name,
            uri = null,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }

    override suspend fun toDownloadedMessageContent(getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo): MessageContent {
        val downloadedUri = getUriFromCloudResourceInfo(
            fileName = file_name.toSafeFilename(),
            cloudType = cloud_type,
            cloudId = cloud_id,
            url = url
        )
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image(
            url = url,
            width = 0,
            height = 0,
            fileName = file_name,
            uri = downloadedUri,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }
}

data class Audio(
    val url: String,
    val cloud_type: Int,
    val cloud_id: String,
    val file_name: String,
    override val type: Int = 3
) : Content {
    override fun toMessageContent(): MessageContent {
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio(
            url = url,
            fileName = file_name,
            uri = null,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }

    override suspend fun toDownloadedMessageContent(getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo): MessageContent {
        val downloadedUri = getUriFromCloudResourceInfo(
            fileName = file_name.toSafeFilename(),
            cloudType = cloud_type,
            cloudId = cloud_id,
            url = url
        )
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio(
            url = url,
            fileName = file_name,
            uri = downloadedUri,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }
}

data class File(
    val url: String,
    val cloud_type: Int,
    val cloud_id: String,
    val file_name: String,
    override val type: Int = 4
) : Content {
    override fun toMessageContent(): MessageContent {
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File(
            url = url,
            name = file_name,
            extension = FileUtil.getExtension(file_name),
            size = 0,
            lastModifiedTime = System.currentTimeMillis(),
            uri = null,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }

    override suspend fun toDownloadedMessageContent(getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo): MessageContent {
        val downloadedUri = getUriFromCloudResourceInfo(
            fileName = file_name.toSafeFilename(),
            cloudType = cloud_type,
            cloudId = cloud_id,
            url = url
        )
        return com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File(
            url = url,
            name = file_name,
            extension = FileUtil.getExtension(file_name),
            size = 0,
            lastModifiedTime = System.currentTimeMillis(),
            uri = downloadedUri,
            cloudType = cloud_type,
            cloudId = cloud_id
        )
    }
}