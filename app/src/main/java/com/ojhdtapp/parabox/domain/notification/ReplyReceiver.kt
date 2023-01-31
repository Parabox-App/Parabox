package com.ojhdtapp.parabox.domain.notification

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.remote.dto.filterMissing
import com.ojhdtapp.parabox.data.remote.dto.saveLocalResourcesToCloud
import com.ojhdtapp.parabox.data.remote.dto.server.ServerSendMessageDto
import com.ojhdtapp.parabox.data.remote.dto.toFcmMessageContentList
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var fcmApiHelper: FcmApiHelper

    @Inject
    lateinit var updateMessage: UpdateMessage

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        GlobalScope.launch(Dispatchers.IO) {
            val contact = intent.getParcelableExtra<Contact>("contact")
            val sendTargetType = intent.getIntExtra("sendTargetType", SendTargetType.GROUP)
            if (contact == null) {
                return@launch
            } else {
                val pluginConnectionEntity = database.contactDao.getPluginConnectionById(contact.senderId)
                val timestamp = System.currentTimeMillis()
                val results = RemoteInput.getResultsFromIntent(intent) ?: return@launch
                // The message typed in the notification reply.
                val input = results.getCharSequence(NotificationUtil.KEY_TEXT_REPLY)?.toString()?: ""
                val contents = listOf(PlainText(text = input))
                val pluginConnection = pluginConnectionEntity.toKitPluginConnection(sendTargetType)
                handleNewMessage(
                    contents,
                    pluginConnection,
                    timestamp,
                    sendTargetType
                ).also {
                    val dto = SendMessageDto(
                        contents = contents,
                        timestamp = timestamp,
                        pluginConnection = pluginConnection,
                        messageId = it
                    )
                    val workingMode =
                        context.dataStore.data.first()[DataStoreKeys.SETTINGS_WORKING_MODE]
                            ?: WorkingMode.NORMAL.ordinal
                    val enableFcm =
                        context.dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
//                        val fcmRole = dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_ROLE]
//                            ?: FcmConstants.Role.SENDER.ordinal
                    when (workingMode) {
                        WorkingMode.NORMAL.ordinal -> {
                            context.startService(PluginService.getReplyIntent(context, dto))
                        }
                        WorkingMode.RECEIVER.ordinal -> {
                            if (enableFcm) {
//                                    val fcmCloudStorage =
//                                        dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE]
//                                            ?: FcmConstants.CloudStorage.NONE.ordinal
                                val dtoWithoutUri = dto.copy(
                                    contents = dto.contents.saveLocalResourcesToCloud(
                                        context
                                    ).filterMissing()
                                )
                                if (fcmApiHelper.pushSendDto(
                                        dtoWithoutUri
                                    )?.isSuccessful == true
                                ) {
                                    updateMessage.verifiedState(it, true)
                                } else {
                                    updateMessage.verifiedState(it, false)
                                }
                            } else {
                                // do nothing
                            }
                        }
                        WorkingMode.FCM.ordinal -> {
                            // to Google server
                            if (enableFcm) {
                                database.fcmMappingDao.getFcmMappingById(dto.pluginConnection.id)
                                    ?.also { fcmMapping ->
                                        val contents = dto.contents.saveLocalResourcesToCloud(
                                            context
                                        ).filterMissing().toFcmMessageContentList()
                                        val dto = ServerSendMessageDto(
                                            contents = contents,
                                            slaveOriginUid = fcmMapping.uid,
                                            timestamp = dto.timestamp
                                        )
                                        val json = Gson().toJson(dto)
                                        val fm = Firebase.messaging
                                        Log.d("parabox", "message from: ${fcmMapping.from}")
                                        fm.send(
                                            RemoteMessage.Builder("${fcmMapping.from}@fcm.googleapis.com")
                                                .setMessageId(it.toString())
                                                .addData("message", json)
                                                .addData("session_id", fcmMapping.sessionId)
                                                .build()
                                        )
                                        delay(500)
                                        updateMessage.verifiedState(it, true)
//                                        if (fcmApiHelper.pushSendDto(
//                                            )?.isSuccessful == true
//                                        ) {
//                                            updateMessage.verifiedState(it, true)
//                                        } else {
//                                            updateMessage.verifiedState(it, false)
//                                        }
                                    }

                            }
                        }
                    }


                    updateNotification(input, timestamp, it, sendTargetType, contact, pluginConnection.connectionType.toString())
                }
            }
        }
    }

    private suspend fun updateNotification(
        input: String,
        timestamp: Long,
        messageId: Long,
        sendTargetType: Int,
        contact: Contact,
        channelId: String,
    ) {
        val message = Message(
            contents = listOf(com.ojhdtapp.parabox.domain.model.message_content.PlainText(input)),
            profile = Profile("您", null, null, null),
            timestamp = timestamp,
            messageId = messageId,
            sentByMe = true,
            verified = false,
            sendType = sendTargetType
        )
        notificationUtil.sendNewMessageNotification(message, contact, channelId, sendTargetType == SendTargetType.GROUP)
    }
}