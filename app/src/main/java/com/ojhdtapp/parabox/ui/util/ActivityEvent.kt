package com.ojhdtapp.parabox.ui.util

import android.content.Intent
import com.ojhdtapp.messagedto.PluginConnection
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.messagedto.message_content.MessageContent

sealed class ActivityEvent{
    data class LaunchIntent(val intent: Intent) : ActivityEvent()
    data class SendMessage(val contents: List<MessageContent>, val pluginConnection: PluginConnection, val sendType: Int): ActivityEvent()
    data class RecallMessage(val type: Int, val messageId: Long) : ActivityEvent()
    object SetUserAvatar: ActivityEvent()
    object StartRecording: ActivityEvent()
    object StopRecording: ActivityEvent()
}
