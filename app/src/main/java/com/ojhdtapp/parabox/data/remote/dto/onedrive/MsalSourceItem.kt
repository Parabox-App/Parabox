package com.ojhdtapp.parabox.data.remote.dto.onedrive

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/7
 * https://docs.microsoft.com/zh-cn/graph/api/resources/driveitem?view=graph-rest-1.0
 **/
// contain @microsoft.graph.downloadUrl
@Keep
data class MsalSourceItem(
    val id: String,
    val createdDateTime: String,
    val lastModifiedDateTime: String,
    val cTag: String,
    val eTag: String,
    val webUrl: String?,
    val name: String,
    val size: Long,
    val file: MsalFileInfo?,
    val folder: MsalFolderInfo?,
    @SerializedName("@microsoft.graph.downloadUrl")
    val downloadUrl: String?
) {
    fun isFolder() = folder != null
}

@Keep
data class MsalFolderInfo(
    val childCount: Long
)

@Keep
data class MsalFileInfo(
    val mimeType: String,
    val hashes: MsalFileHashes
)

@Keep
data class MsalFileHashes(
    val quickXorHash: String,
    val sha1Hash: String,
    val sha256Hash: String
)