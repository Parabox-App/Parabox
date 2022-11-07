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
    val connectionType: Int,
    val connectionName: String,
    val author: String,
    val description: String,
    val plainTextSupport: Int,
    val imageSupport: Int,
    val audioSupport: Int,
    val fileSupport: Int,
    val atSupport: Int,
    val quoteReplySupport: Int,
){
    companion object{
        const val RUNNING_STATUS_CHECKING = 0
        const val RUNNING_STATUS_DISABLED = 1
        const val RUNNING_STATUS_RUNNING = 2
        const val RUNNING_STATUS_ERROR = 3
        const val SUPPORT_NULL = 0
        const val SUPPORT_RECEIVE = 1
        const val SUPPORT_ALL = 2
    }
}