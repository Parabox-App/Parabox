package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ojhdtapp.parabox.R
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.DecimalFormat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun String.toSafeFilename(): String {
    return this.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
}

fun buildFileName(type: String, extension: String): String{
    return "${type}_${System.currentTimeMillis().toDateAndTimeString()}.${extension}"
}

fun Long.toSizeString(): String {
    val format = DecimalFormat("#.##")
    return when (this.coerceAtLeast(0)) {
        in 0 until 1024 -> "${this}B"
        in 1024 until 1048576 -> "${format.format(this.toDouble() / 1024)}KB"
        in 1048576 until 1073741824 -> "${format.format(this.toDouble() / 1048576)}MB"
        else -> "${format.format(this.toDouble() / 1073741824)}GB"
    }
}

suspend fun Flow<ParaboxCloudStatus>.awaitUntilSuccess(timeoutMills: Long? = 0L): ParaboxCloudStatus.Synced? {
    return withTimeoutOrNull(timeoutMills ?: 15000L) {
        suspendCoroutine<ParaboxCloudStatus.Synced> { cot ->
            launch {
                this@awaitUntilSuccess.collectLatest {
                    if (it is ParaboxCloudStatus.Synced) {
                        cot.resume(it)
                    }
                }
            }
        }
    }
}