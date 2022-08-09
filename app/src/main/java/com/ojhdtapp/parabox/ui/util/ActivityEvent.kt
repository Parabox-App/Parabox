package com.ojhdtapp.parabox.ui.util

import android.content.Intent

sealed class ActivityEvent{
    data class LaunchIntent(val intent: Intent) : ActivityEvent()
}
