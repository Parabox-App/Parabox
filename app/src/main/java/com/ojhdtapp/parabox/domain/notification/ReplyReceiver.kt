package com.ojhdtapp.parabox.domain.notification

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var repository: MainRepository

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
//        val contactId = intent.getLongExtra("contactId", -1L)
        GlobalScope.launch(Dispatchers.IO) {
            val senderId = intent.getLongExtra("senderId", -1L)
            val sendTargetType = intent.getIntExtra("sendTargetType", SendTargetType.GROUP)
            if (senderId == -1L) {
                Log.e("ReplyReceiver", "senderId is null")
                return@launch
            } else {
                val pluginConnectionEntity = database.contactDao.getPluginConnectionById(senderId)
                val timestamp = System.currentTimeMillis()
                val results = RemoteInput.getResultsFromIntent(intent) ?: return@launch
                // The message typed in the notification reply.
                val input = results.getCharSequence(NotificationUtil.KEY_TEXT_REPLY)?.toString()
                val contents = listOf(PlainText(text = input ?: ""))
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
                    context.startService(PluginService.getReplyIntent(context, dto))
                }
            }
        }
    }
}