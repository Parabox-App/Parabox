package com.ojhdtapp.parabox.domain.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LifecycleService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        Log.d("parabox", "Refreshed token: $token")
        GlobalScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.FCM_TOKEN] = token
            }
        }
        super.onNewToken(token)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("parabox", "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val dtoJson = remoteMessage.data["dto"]
            when (type) {
                "receive" -> {
                    val dto = dtoJson?.let { gson.fromJson(it, ReceiveMessageDto::class.java) }
                    Log.d("parabox", "Message data payload: $dto")
                    dto?.also {
                        GlobalScope.launch(Dispatchers.IO) {
                            val workingMode = dataStore.data.map { preferences ->
                                preferences[DataStoreKeys.SETTINGS_WORKING_MODE]
                                    ?: WorkingMode.NORMAL.ordinal
                            }.first()
                            when (workingMode) {
                                WorkingMode.NORMAL.ordinal -> {

                                }
                                WorkingMode.RECEIVER.ordinal -> {
                                    handleNewMessage(it)
                                }
                                WorkingMode.FCM.ordinal -> {

                                }
                            }
                        }
                    }
                }

                "send" -> {
                    val dto = dtoJson?.let { gson.fromJson(it, SendMessageDto::class.java) }
                    Log.d("parabox", "Message data payload: $dto")
                    dto?.also {
                        GlobalScope.launch(Dispatchers.IO) {
                            val workingMode = dataStore.data.map { preferences ->
                                preferences[DataStoreKeys.SETTINGS_WORKING_MODE]
                                    ?: WorkingMode.NORMAL.ordinal
                            }.first()
                            when (workingMode) {
                                WorkingMode.NORMAL.ordinal -> {
                                    val downloadedContent = dto.contents.map {
                                        when (it) {
                                            is Image -> {
                                                val downloadedUri = getUriFromCloudResourceInfo(
                                                    fileName = it.fileName ?: "Image_${
                                                        System.currentTimeMillis()
                                                            .toDateAndTimeString()
                                                    }.png",
                                                    cloudType = it.cloudType
                                                        ?: FcmConstants.CloudStorage.NONE.ordinal,
                                                    url = it.url, cloudId = it.cloudId
                                                )
                                                it.copy(uri = downloadedUri)
                                            }
                                            is Audio -> {
                                                val downloadedUri = getUriFromCloudResourceInfo(
                                                    fileName = it.fileName ?: "Audio_${
                                                        System.currentTimeMillis()
                                                            .toDateAndTimeString()
                                                    }.mp3",
                                                    cloudType = it.cloudType
                                                        ?: FcmConstants.CloudStorage.NONE.ordinal,
                                                    url = it.url, cloudId = it.cloudId
                                                )
                                                it.copy(uri = downloadedUri)
                                            }
                                            is File -> {
                                                val downloadedUri = getUriFromCloudResourceInfo(
                                                    fileName = it.name,
                                                    cloudType = it.cloudType
                                                        ?: FcmConstants.CloudStorage.NONE.ordinal,
                                                    url = it.url, cloudId = it.cloudId
                                                )
                                                it.copy(uri = downloadedUri)
                                            }
//                                        is Image -> {
//                                            val downloadedUri =
//                                                it.url?.let { url ->
//                                                    downloadUtil.downloadUrl(
//                                                        url,
//                                                        it.fileName ?:"Image_${
//                                                            System.currentTimeMillis()
//                                                                .toDateAndTimeString()
//                                                        }.jpg",
//                                                        baseContext.getExternalFilesDir("chat")!!
//                                                    )?.let {
//                                                        FileUtil.getUriOfFile(baseContext, it)
//                                                    }
//                                                }
//                                            it.copy(uri = downloadedUri)
//                                        }
//
//                                        is Audio -> {
//                                            val downloadedUri =
//                                                it.url?.let { url ->
//                                                    downloadUtil.downloadUrl(
//                                                        url,
//                                                        it.fileName?: "Audio_${
//                                                            System.currentTimeMillis()
//                                                                .toDateAndTimeString()
//                                                        }.mp3",
//                                                        baseContext.getExternalFilesDir("chat")!!
//                                                    )?.let {
//                                                        FileUtil.getUriOfFile(baseContext, it)
//                                                    }
//                                                }
//                                            it.copy(uri = downloadedUri)
//                                        }

                                            else -> it
                                        }
                                    }
                                    Log.d("parabox", "downloadedContent: $downloadedContent")
                                    handleNewMessage(
                                        downloadedContent,
                                        dto.pluginConnection,
                                        dto.timestamp,
                                        dto.pluginConnection.connectionType
                                    ).also {
                                        // Update messageId to latest
                                        bindOnceAndSend(
                                            dto.copy(
                                                contents = downloadedContent,
                                                messageId = it
                                            )
                                        )
                                    }
                                }
                                WorkingMode.RECEIVER.ordinal -> {

                                }
                                WorkingMode.FCM.ordinal -> {

                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
        super.onMessageReceived(remoteMessage)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private fun bindOnceAndSend(dto: SendMessageDto) {
        bindService(
            Intent(this, PluginService::class.java),
            object : ServiceConnection {
                override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                    val service = (p1 as PluginService.PluginServiceBinder).getService()
                    service.getAppModelList().map { it.packageName }.forEach { packageName ->
                        dto.contents.map {
                            when (it) {
                                is Image -> {
                                    grantUriPermission(
                                        packageName,
                                        it.uri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                is Audio -> {
                                    grantUriPermission(
                                        packageName,
                                        it.uri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                is File -> {
                                    grantUriPermission(
                                        packageName,
                                        it.uri,
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                    service.sendMessage(dto)
                    unbindService(this)
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                }
            },
            BIND_AUTO_CREATE
        )
    }

    suspend fun getUriFromCloudResourceInfo(
        fileName: String,
        cloudType: Int,
        url: String?,
        cloudId: String?
    ): Uri? {
        return when (cloudType) {
            FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> {
                cloudId?.let {
                    GoogleDriveUtil.downloadFile(
                        baseContext,
                        it,
                        baseContext.externalCacheDir!!
                    )
                }?.let {
                    FileUtil.getUriOfFile(baseContext, it)
                }
            }

            FcmConstants.CloudStorage.TENCENT_COS.ordinal -> {
                cloudId?.let { cosPath ->
                    val secretId =
                        dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_ID]
                    val secretKey =
                        dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_KEY]
                    val bucket =
                        dataStore.data.first()[DataStoreKeys.TENCENT_COS_BUCKET]
                    val region =
                        dataStore.data.first()[DataStoreKeys.TENCENT_COS_REGION]
                    if (secretId != null && secretKey != null && bucket != null && region != null) {
                        val res = TencentCOSUtil.downloadFile(
                            baseContext,
                            secretId,
                            secretKey,
                            region,
                            bucket,
                            cosPath,
                            baseContext.externalCacheDir!!.absolutePath,
                            fileName
                        )
                        if (res) {
                            FileUtil.getUriOfFile(
                                baseContext,
                                java.io.File(baseContext.externalCacheDir!!, fileName)
                            )
//                            val file = baseContext.getExternalFilesDir("chat")!!.listFiles { file ->
//                                file.name == fileName
//                            }?.firstOrNull()
//                            if (file != null) {
//                                FileUtil.getUriOfFile(
//                                    baseContext,
//                                    file
//                                )
//                            } else null
                        } else null
                    } else null
                } ?: url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        baseContext.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(baseContext, it)
                    }
                }
            }

            FcmConstants.CloudStorage.QINIU_KODO.ordinal -> {
                cloudId?.let { key ->
                    val accessKey =
                        dataStore.data.first()[DataStoreKeys.QINIU_KODO_ACCESS_KEY]
                    val secretKey =
                        dataStore.data.first()[DataStoreKeys.QINIU_KODO_SECRET_KEY]
                    val bucket =
                        dataStore.data.first()[DataStoreKeys.QINIU_KODO_BUCKET]
                    val domain =
                        dataStore.data.first()[DataStoreKeys.QINIU_KODO_DOMAIN]
                    if (accessKey != null && secretKey != null && bucket != null && domain != null) {
                        QiniuKODOUtil.downloadFile(domain, accessKey, secretKey, key)?.let{ newUrl ->
                            downloadUtil.downloadUrl(
                                newUrl,
                                fileName,
                                baseContext.externalCacheDir!!
                            )?.let {
                                FileUtil.getUriOfFile(baseContext, it)
                            }
                        }
                    } else null
                } ?: url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        baseContext.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(baseContext, it)
                    }
                }
            }

            else -> {
                url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        baseContext.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(baseContext, it)
                    }
                }
            }
        }
    }
}