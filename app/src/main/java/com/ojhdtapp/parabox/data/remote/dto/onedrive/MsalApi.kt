package com.ojhdtapp.parabox.data.remote.dto.onedrive

import com.ojhdtapp.parabox.core.util.OnedriveUtil.Companion.APP_ROOT_DIR
import com.ojhdtapp.parabox.core.util.OnedriveUtil.Companion.TOKEN_KEY
import retrofit2.http.*
import retrofit2.Response

/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
interface MsalApi {
    /**
     * @param itemPath 云端的路径，如：/foo.txtNet
     */
    @POST("users/{user-id}/drive/special/$APP_ROOT_DIR:/{item-path}:/createUploadSession")
    suspend fun createUploadSession(
        @Header(TOKEN_KEY) authorization: String,
        @Path("user-id") userId: String,
        @Path("item-path") itemPath: String
    ): MsalUploadSession

    /**
     * 获取驱动器列表，也就是onedrive 空间信息
     */
    @GET("users/{userId}/drives")
    suspend fun getDriveList(
        @Header(TOKEN_KEY) authorization: String,
        @Path("userId") userId: String
    ): MsalResponse<List<DriveItem>>

    /**
     * 获取应用的app子文件夹列表
     */
    @GET("users/{user-id}/drive/items/{item-id}/children")
    suspend fun getFolderListById(
        @Header(TOKEN_KEY) authorization: String,
        @Path("user-id") userId: String,
        @Path("item-id") itemId: String
    ): MsalResponse<List<MsalSourceItem>>

    /**
     * 获取应用的app文件夹列表
     */
    @GET("users/{userId}/drive/special/$APP_ROOT_DIR/children")
    suspend fun getAppFolderList(
        @Header(TOKEN_KEY) authorization: String,
        @Path("userId") userId: String
    ): MsalResponse<List<MsalSourceItem>>

    /**
     * 获取单个文件信息
     * @param itemId 文件id
     */
    @GET("users/{user-id}/drive/items/{item-id}")
    suspend fun getFileInfoById(
        @Header(TOKEN_KEY) authorization: String,
        @Path("user-id") userId: String,
        @Path("item-id") itemId: String
    ): MsalSourceItem?

    /**
     * 获取单个文件信息
     * @param itemPath 文件在云盘的相对路径，如：/xxx.zip
     */
    @GET("users/{user-id}/drive/special/$APP_ROOT_DIR:/{item-path}")
    suspend fun getFileInfoByPath(
        @Header(TOKEN_KEY) authorization: String,
        @Path("user-id") userId: String,
        @Path("item-path") itemPath: String
    ): MsalSourceItem?

    /**
     * 删除文件，如果成功，此调用将返回 204 No Content 响应，以指明资源已被删除，没有可返回的内容。
     */
    @DELETE("users/{userId}/drive/items/{itemId}")
    suspend fun deleteFile(
        @Header(TOKEN_KEY) authorization: String,
        @Path("userId") userId: String,
        @Path("itemId") itemId: String
    ): Response<Void>
}