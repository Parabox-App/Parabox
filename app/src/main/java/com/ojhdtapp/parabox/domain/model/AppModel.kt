package com.ojhdtapp.parabox.domain.model

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppModel(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    val version: String,
    val launchIntent: Intent?,
    val runningStatus: Int,
    val connectionType: Int
){
    companion object{
        const val RUNNING_STATUS_CHECKING = 0
        const val RUNNING_STATUS_DISABLED = 1
        const val RUNNING_STATUS_RUNNING = 2
        const val RUNNING_STATUS_ERROR = 3
    }
}