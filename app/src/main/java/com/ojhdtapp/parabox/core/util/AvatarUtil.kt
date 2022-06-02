package com.ojhdtapp.parabox.core.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

fun ByteArray.toAvatarBitmap(): ImageBitmap{
    return BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}