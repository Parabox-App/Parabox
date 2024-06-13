package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ojhdtapp.parabox.R
import java.text.DecimalFormat

fun String.toSafeFilename(): String {
    return this.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
}

fun Uri.checkUriAvailable(context: Context): Boolean {
    return try {
        val parcelFileDescriptor: ParcelFileDescriptor? =
            context.contentResolver.openFileDescriptor(this, "r")
        parcelFileDescriptor?.close()
        true
    } catch (e: Exception) {
        false
    }
}

fun Uri.replacedIfUnavailable(context: Context): Any {
    return if (!checkUriAvailable(context)) {
        R.drawable.image_lost
    } else this
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