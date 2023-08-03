package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ojhdtapp.parabox.R

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