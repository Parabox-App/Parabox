package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

interface ParaboxCloudService {
    suspend fun upload() : ParaboxResourceInfo.ParaboxRemoteInfo
    suspend fun download(): ParaboxResourceInfo.ParaboxLocalInfo
}