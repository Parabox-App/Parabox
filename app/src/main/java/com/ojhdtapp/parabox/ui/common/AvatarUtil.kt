package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import java.util.*


fun ByteArray.toAvatarBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}

object AvatarUtil {
    fun createNamedAvatarBm(
        width: Int = 150, height: Int = 150,
        backgroundColor: Int,
        textColor: Int,
        name: String?
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, width, height)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint()
        backgroundPaint.color = backgroundColor
        canvas.drawRect(rect, backgroundPaint)

        val textPaint = Paint()
        textPaint.color = textColor
        textPaint.textSize = 72f
        textPaint.textScaleX = 1f
        textPaint.textAlign = Paint.Align.CENTER
        if (name != null) {
            val baselineY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(name, rect.centerX().toFloat(), baselineY, textPaint)
        }
        return bitmap
    }

    fun Bitmap.getCircledBitmap(): Bitmap {
        val output = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
//        val output = copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, this.width, this.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(this.width / 2f, this.height / 2f, this.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, rect, rect, paint)
        return output
    }
}