package com.ojhdtapp.parabox.ui.util

import android.content.Intent
import com.ojhdtapp.messagedto.SendMessageDto

sealed class ActivityEvent{
    data class LaunchIntent(val intent: Intent) : ActivityEvent()
    data class SendMessage(val dto: SendMessageDto): ActivityEvent()
}
