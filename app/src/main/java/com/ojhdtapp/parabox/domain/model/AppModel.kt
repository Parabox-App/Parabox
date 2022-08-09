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
){
    companion object{
        val RUNNING_STATUS_DISABLED = 1
        val RUNNING_STATUS_RUNNING = 2
        val RUNNING_STATUS_ERROR = 3
    }
}