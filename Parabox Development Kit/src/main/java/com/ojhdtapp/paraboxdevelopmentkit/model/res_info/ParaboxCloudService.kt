package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import android.net.Uri

interface ParaboxCloudService {
    suspend fun upload(uri: Uri): ParaboxResourceInfo.ParaboxRemoteInfo
    suspend fun download(url: String): ParaboxResourceInfo.ParaboxLocalInfo
    suspend fun download(
        uuid: String,
        cloudPath: String,
        driveType: Int
    ): ParaboxResourceInfo.ParaboxLocalInfo
}