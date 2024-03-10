package com.ojhdtapp.parabox.core.util

import android.graphics.Bitmap

object ImageUtil {
    fun checkBitmapLight(bitmap: Bitmap, skipStep: Int = 2): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var lightPixelCount = 0
        var darkPixelCount = 0
        for (i in pixels.indices step skipStep) {
            val pixel = pixels[i]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
            if (luminance > 192) {
                lightPixelCount++
            } else {
                darkPixelCount++
            }
        }
        val lightRatio = lightPixelCount.toFloat() / pixels.size
        val darkRatio = darkPixelCount.toFloat() / pixels.size
        val isLight = lightRatio > darkRatio
        return isLight
    }
}