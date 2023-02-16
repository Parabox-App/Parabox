package com.ojhdtapp.parabox.core.util

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.remote.dto.onedrive.DriveItem
import com.ojhdtapp.parabox.data.remote.dto.onedrive.MsalApi
import com.ojhdtapp.parabox.data.remote.dto.onedrive.MsalSourceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import javax.inject.Inject

class OnedriveUtil @Inject constructor(
    val context: Context,
    val msalApi: MsalApi
) {
    companion object {
        const val SERVICE_CODE = 1002
        const val APP_ROOT_DIR = "approot"
        const val TOKEN_KEY = "Authorization"
        const val BASE_URL = "https://graph.microsoft.com/v1.0/"

        const val STATUS_SUCCESS = 1
        const val STATUS_ERROR = 2
        const val STATUS_CANCEL = 3
    }
    private var mSingleAccountApp : ISingleAccountPublicClientApplication? = null
    private var authInfo: IAuthenticationResult? = null
    private val scopes = listOf<String>("User.Read", "Files.ReadWrite.All")
    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    exception.printStackTrace()
                }
            })
    }

    /**
     * Load currently signed-in accounts, if there's any.
     **/
    fun loadAccounts() {
        mSingleAccountApp?.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount != null) {
                    getTokenByAccountInfo(activeAccount)
                }
                Log.d("MSAL", "Account loaded: " + activeAccount?.username)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                // Handle the change
                if (currentAccount != null) {
                    getTokenByAccountInfo(currentAccount)
                }
            }

            override fun onError(exception: MsalException) {
                exception.printStackTrace()
            }
        })
    }


    fun signIn(
        activity: Activity,
        onResult: (code: Int) -> Unit
    ){
        if(authInfo != null){
            onResult(STATUS_CANCEL)
            return
        }
        val callback = object: AuthenticationCallback{
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authInfo = authenticationResult
                onResult(STATUS_SUCCESS)
            }

            override fun onError(exception: MsalException?) {
                exception?.printStackTrace()
                onResult(STATUS_ERROR)
            }

            override fun onCancel() {
                onResult(STATUS_CANCEL)
            }

        }
        mSingleAccountApp?.signIn(SignInParameters.builder()
            .withActivity(activity)
            .withScopes(scopes)
            .withCallback(callback)
            .build())
    }

    fun signOut(
        onResult: (code: Int) -> Unit
    ){
        mSingleAccountApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                Log.d("MSAL", "Signed Out")
                onResult(STATUS_SUCCESS)
                authInfo = null
            }
            override fun onError(exception: MsalException) {
                exception.printStackTrace()
                onResult(STATUS_ERROR)
            }
        })
    }

    fun acquireToken(scopes: List<String>){
        val callback = object: AuthenticationCallback{
            override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                authenticationResult?.accessToken
            }

            override fun onError(exception: MsalException?) {
            }

            override fun onCancel() {
            }
        }
        mSingleAccountApp?.acquireTokenSilent(
            AcquireTokenSilentParameters.Builder()
                .withScopes(scopes)
                .withCallback(callback)
                .build()
        )
    }
    private fun getTokenByAccountInfo(account: IAccount) {
        mSingleAccountApp?.acquireTokenSilentAsync(
            AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .withScopes(scopes)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                        authInfo = authenticationResult
                    }
                    override fun onError(exception: MsalException) {
                        exception.printStackTrace()
                        signOut(){}
                    }
                })
                .build()
        )
    }

    suspend fun getDriveList(): List<DriveItem>? {
        if (authInfo == null) {
            return null
        }
        val response = msalApi.getDriveList(authInfo!!.accessToken, authInfo?.account?.id ?: "")
        if (response.value == null) {
            return null
        }
        return response.value
    }

    suspend fun getFileList(path: String): List<MsalSourceItem>? {
        if (authInfo == null) {
            return null
        }
        val response = if (path == "/") {
            msalApi.getAppFolderList(authInfo!!.accessToken, authInfo?.account?.id ?: "")
        } else {
            msalApi.getFolderListById(authInfo!!.accessToken, authInfo?.account?.id ?: "", path)
        }
        if (response.value == null) {
            return null
        }
        return response.value
    }

    suspend fun getFileInfo(fileKey: String): MsalSourceItem? {
        val userId = authInfo?.account?.id ?: ""
        Log.d("OneDrive", "getFileInfo, userId = ${userId}, fileKey = $fileKey")

        try {
            val response: MsalSourceItem? = if (fileKey.startsWith("/")) {
                msalApi.getFileInfoByPath(
                        authInfo!!.accessToken,
                        userId,
                        fileKey.substring(1, fileKey.length)
                    )
            } else {
                msalApi.getFileInfoById(authInfo!!.accessToken, userId, fileKey)
            }
            if (response == null) {
                return null
            }
            return response
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 如果成功，此调用将返回 204 No Content 响应，以指明资源已被删除，没有可返回的内容。
     */
    suspend fun delFile(fileKey: String): Boolean {
        if(authInfo == null) {
            return false
        }
        val userId = authInfo?.account?.id ?: ""
        val response = msalApi
            .deleteFile(authInfo!!.accessToken, userId, fileKey)
        return response.code() == HttpURLConnection.HTTP_NO_CONTENT
    }


    suspend fun uploadFile(
        context: Context,
        file: File
    ): Boolean {
        if(authInfo == null) return false
        val userId = authInfo?.account?.id ?: ""
        try {
            // 创建上传session
            val uploadSession = msalApi.createUploadSession(
                    authorization = authInfo!!.accessToken,
                    userId = userId,
                    itemPath = file.name
                )

            // 开始上传文件
            val desc = file.asRequestBody("multipart/form-data".toMediaType())
            val body = MultipartBody.Part.createFormData("file", file.name, desc)
            val fileSize = file.length()
            val range = "bytes 0-${fileSize - 1}/${fileSize}"
            val response = msalApi.uploadFile(
                url = uploadSession.uploadUrl,
                contentLength = fileSize,
                contentRange = range,
                body = body.body
            )

//            val request = Request.Builder()
//                .header("Content-Length", fileSize.toString())
//                .header("Content-Range", "bytes 0-${fileSize - 1}/${fileSize}")
//                .url(uploadSession.uploadUrl)
//                .put(body = body.body)
//                .build()
//            val response = okClient.newCall(request)
//                .execute()
//            if (response.code != HttpURLConnection.HTTP_OK && response.code != HttpURLConnection.HTTP_CREATED) {
//                Log.d("Onedrive", "上传失败，code = ${response.code}, msg = ${response.message}")
//                return false
//            }
//            val responseBytes = response.body?.bytes()
            if (response == null) {
                return false
            }
//            val responseContent = String(responseBytes, Charset.forName("UTF-8"))
//            val obj = Gson().fromJson(responseContent, MsalSourceItem::class.java)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun downloadFile(
        context: Context,
        cloudPath: String,
        filePath: File
    ): String? {
        return withContext(Dispatchers.IO) {
//            val hb = Headers.Builder()
//                .add(TOKEN_KEY, getAuthInfo().accessToken)
//                .build()
//            val request: Request = Request.Builder()
//                .url("$BASE_URL/users/${getUserId()}/drive/items/${dbRecord.cloudDiskPath}/content")
//                .headers(hb)
//                .build()
//            val call = netManager.getClient()
//                .newCall(request)

            try {
                val response = msalApi.downloadFile(
                    authorization = authInfo!!.accessToken,
                    userId = authInfo?.account?.id ?: "",
                    itemPath = cloudPath
                )
                if (!response.isSuccessful) {
                    return@withContext null
                }
                val byteSystem = response.body() ?: return@withContext null
                FileUtils.writeByteArrayToFile(filePath, byteSystem)
                return@withContext filePath.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}