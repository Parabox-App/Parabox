package com.ojhdtapp.parabox.ui.util

import android.content.Intent
import android.net.Uri
import com.ojhdtapp.messagedto.PluginConnection
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.messagedto.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.File

sealed interface ActivityEvent {
    data class LaunchIntent(val intent: Intent) : ActivityEvent
    data class SendMessage(
        val contents: List<MessageContent>,
        val pluginConnection: PluginConnection,
        val sendType: Int
    ) : ActivityEvent

    data class RecallMessage(val type: Int, val messageId: Long) : ActivityEvent
    object SetUserAvatar : ActivityEvent
    object StartRecording : ActivityEvent
    object StopRecording : ActivityEvent
    data class StartAudioPlaying(val uri: Uri? = null, val url: String? = null) : ActivityEvent
    object PauseAudioPlaying : ActivityEvent
    object ResumeAudioPlaying : ActivityEvent
    data class SetAudioProgress(val fraction: Float) : ActivityEvent
    object StopAudioPlaying : ActivityEvent
    data class DownloadFile(val file: File) : ActivityEvent
    data class OpenFile(val file: File) : ActivityEvent
    object Vibrate : ActivityEvent
    object RefreshMessage: ActivityEvent
}
