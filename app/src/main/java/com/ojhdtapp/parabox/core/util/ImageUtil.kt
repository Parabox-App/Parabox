package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ojhdtapp.parabox.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object ImageUtil {
    private val imageBitmap2UriMap = ConcurrentHashMap<String, Uri>()

    fun createNamedAvatarBm(
        width: Int = 150, height: Int = 150,
        backgroundColor: Int,
        textColor: Int,
        name: String?
    ): Bitmap {
        val shortName = name?.takeIf { it.isNotEmpty() }?.substring(0, 1)
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
        if (shortName != null) {
            val baselineY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(shortName, rect.centerX().toFloat(), baselineY, textPaint)
        }
        return bitmap
    }

    fun ByteArray.toAvatarBitmap(): ImageBitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
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

    suspend fun getBitmapWithCoil(context: Context, model: Any?) : Bitmap? {
        Log.d("parabox", "getBitmapWithCoil: $model")
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as BitmapDrawable).bitmap
            } else {
                (result as ErrorResult).throwable.printStackTrace()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageUriFromBitmapWithCache(context: Context, bm: Bitmap, cacheKey: String? = null): Uri? {
        if (cacheKey != null && imageBitmap2UriMap.containsKey(cacheKey)) {
            return imageBitmap2UriMap[cacheKey]
        }
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        targetDir.listFiles()?.sortedByDescending { it.lastModified() }
            ?.forEachIndexed() { index, file ->
                if (index > 20) {
                    file.delete()
                }
            }
        return getImageUriFromBitmap(context, bm)?.also {
            if (cacheKey != null) {
                imageBitmap2UriMap[cacheKey] = it
            }
        }
    }

    fun getImageUriFromBitmap(context: Context, bm: Bitmap): Uri? {
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile =
            File(targetDir, buildFileName(FileUtil.DEFAULT_IMAGE_NAME, FileUtil.DEFAULT_IMAGE_EXTENSION))
        val bytes = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fileOutPut = FileOutputStream(tempFile).use {
            it.write(bitmapData)
        }
        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider", tempFile
        )
    }
}