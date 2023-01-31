package com.ojhdtapp.parabox.domain.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.FileUtil.toSafeFilename
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.FcmMapping
import com.ojhdtapp.parabox.data.local.entity.FcmMappingSessionIdUpdate
import com.ojhdtapp.parabox.data.remote.dto.server.ServerReceiveMessageDto
import com.ojhdtapp.parabox.data.remote.dto.server.content.toDownloadedMessageContentList
import com.ojhdtapp.parabox.data.remote.dto.server.content.toMessageContentList
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.use_case.GetUriFromCloudResourceInfo
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var getUriFromCloudResourceInfo: GetUriFromCloudResourceInfo

    @Inject
    lateinit var updateMessage: UpdateMessage


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
        Log.d("parabox", "Message data payload: ${remoteMessage.data}")
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val dtoJson = remoteMessage.data["dto"]
            val sessionId = remoteMessage.data["ws_session_id"]
            Log.d("parabox", "type: $type")
//            Log.d("parabox", "dto: $dtoJson")
            Log.d("parabox", "sessionId: $sessionId")

            when (type) {
                "receive" -> {
                    val dto = dtoJson?.let { gson.fromJson(it, ReceiveMessageDto::class.java) }
                    Log.d("parabox", "Message data payload: $dto")
                    dto?.also {
                        GlobalScope.launch(Dispatchers.IO) {
                            handleNewMessage(it)
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
                            if (workingMode == WorkingMode.NORMAL.ordinal) {
                                val downloadedContent = dto.contents.map {
                                    when (it) {
                                        is Image -> {
                                            val downloadedUri =
                                                getUriFromCloudResourceInfo(
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
                                            val downloadedUri =
                                                getUriFromCloudResourceInfo(
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
                                            val downloadedUri =
                                                getUriFromCloudResourceInfo(
                                                    fileName = it.name,
                                                    cloudType = it.cloudType
                                                        ?: FcmConstants.CloudStorage.NONE.ordinal,
                                                    url = it.url, cloudId = it.cloudId
                                                )
                                            it.copy(uri = downloadedUri)
                                        }
                                        else -> it
                                    }
                                }
//                                Log.d("parabox", "downloadedContent: $downloadedContent")
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
                            } else {
                                Toast.makeText(
                                    baseContext,
                                    "接收到待处理的发送请求，请转到扩展模式",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                "server" -> {
//                    Log.d("parabox", dtoJson.toString())
                    if (dtoJson != null && remoteMessage.from != null) {

                        val dto = gson.fromJson(dtoJson, ServerReceiveMessageDto::class.java)
                        Log.d("parabox", "Message data payload: $dto")
                        GlobalScope.launch {
                            val fcmMappingId =
                                database.fcmMappingDao.getFcmMappingByUid(dto.slaveOriginUid)?.id?.also{
                                    database.fcmMappingDao.updateSessionId(
                                        FcmMappingSessionIdUpdate(
                                            id = it,
                                            sessionId = sessionId!!
                                        )
                                    )
                                }
                                    ?: database.fcmMappingDao.insertFcmMapping(
                                        FcmMapping(
                                            from = remoteMessage.senderId!!,
                                            uid = dto.slaveOriginUid,
                                            sessionId = sessionId!!
                                        )
                                    )
                            val shouldDownloadCloudResource = dataStore.data.map { preferences ->
                                preferences[DataStoreKeys.SETTINGS_FCM_ENABLE_CACHE]
                                    ?: false
                            }.first()
                            val messageContents = if (shouldDownloadCloudResource) {
                                dto.contents.toDownloadedMessageContentList(
                                    getUriFromCloudResourceInfo = getUriFromCloudResourceInfo
                                )
                            } else {
                                dto.contents.toMessageContentList()
                            }
                            val profile = Profile(
                                name = dto.profile.name,
                                avatar = dto.profile.avatar,
                                id = null,
                                avatarUri = getUriFromCloudResourceInfo(
                                    fileName = "${dto.profile.name.toSafeFilename()}.png",
                                    cloudType = dto.profile.avatar_cloud_type,
                                    url = dto.profile.avatar,
                                    cloudId = dto.profile.avatar_cloud_id
                                )
                            )
                            val subjectProfile = Profile(
                                name = dto.subjectProfile.name,
                                avatar = dto.subjectProfile.avatar,
                                id = fcmMappingId,
                                avatarUri = getUriFromCloudResourceInfo(
                                    fileName = "${dto.subjectProfile.name.toSafeFilename()}.png",
                                    cloudType = dto.subjectProfile.avatar_cloud_type,
                                    url = dto.subjectProfile.avatar,
                                    cloudId = dto.subjectProfile.avatar_cloud_id
                                )
                            )
                            val receiveMessageDto = ReceiveMessageDto(
                                contents = messageContents,
                                profile = profile,
                                subjectProfile = subjectProfile,
                                timestamp = dto.timestamp,
                                messageId = null,
                                pluginConnection = PluginConnection(
                                    connectionType = FcmConstants.CONNECTION_TYPE,
                                    sendTargetType = dto.chatType,
                                    id = fcmMappingId
                                )
                            )
                            handleNewMessage(receiveMessageDto)
                        }
                    }

                }

                else -> {

                }
            }
        }
        super.onMessageReceived(remoteMessage)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("parabox", "onMessageSent: $msgId")
            updateMessage.verifiedState(msgId.toLong(), true)
        }
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("parabox", "onSendError: $msgId")
            updateMessage.verifiedState(msgId.toLong(), false)
        }
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
}