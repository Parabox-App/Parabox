package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor

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