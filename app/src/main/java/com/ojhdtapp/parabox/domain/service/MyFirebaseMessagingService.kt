package com.ojhdtapp.parabox.domain.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
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
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.DownloadUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
                            val fcmRole = dataStore.data.map { preferences ->
                                preferences[DataStoreKeys.SETTINGS_FCM_ROLE]
                                    ?: FcmConstants.Role.SENDER.ordinal
                            }.first()
                            if (fcmRole == FcmConstants.Role.RECEIVER.ordinal) {
                                handleNewMessage(it)
                            }
                        }
                    }
                }

                "send" -> {
                    val dto = dtoJson?.let { gson.fromJson(it, SendMessageDto::class.java) }
                    Log.d("parabox", "Message data payload: $dto")
                    dto?.also {
                        GlobalScope.launch(Dispatchers.IO) {
                            val fcmRole = dataStore.data.map { preferences ->
                                preferences[DataStoreKeys.SETTINGS_FCM_ROLE]
                                    ?: FcmConstants.Role.SENDER.ordinal
                            }.first()
                            if (fcmRole == FcmConstants.Role.SENDER.ordinal) {
                                val downloadedContent = dto.contents.map {
                                    when (it) {
                                        is Image -> {
                                            val downloadedUri =
                                                it.url?.let { url ->
                                                    downloadUtil.downloadUrl(
                                                        url,
                                                        "Image_${
                                                            System.currentTimeMillis()
                                                                .toDateAndTimeString()
                                                        }.jpg",
                                                        baseContext.getExternalFilesDir("chat")!!
                                                    )?.let {
                                                        FileUtil.getUriOfFile(baseContext, it)
                                                    }
                                                }
                                            it.copy(uri = downloadedUri)
                                        }

                                        is Audio -> {
                                            val downloadedUri =
                                                it.url?.let { url ->
                                                    downloadUtil.downloadUrl(
                                                        url,
                                                        "Audio_${
                                                            System.currentTimeMillis()
                                                                .toDateAndTimeString()
                                                        }.mp3",
                                                        baseContext.getExternalFilesDir("chat")!!
                                                    )?.let {
                                                        FileUtil.getUriOfFile(baseContext, it)
                                                    }
                                                }
                                            it.copy(uri = downloadedUri)
                                        }

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
                                    bindOnceAndSend(dto.copy(
                                        contents = downloadedContent,
                                        messageId = it
                                    ))
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
}