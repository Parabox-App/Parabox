package com.ojhdtapp.parabox.core.util

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.*

object FileUtil {

    fun createTmpFileFromUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
            val file = File.createTempFile(fileName, "", context.cacheDir)
            org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, file)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriFromBitmap(context: Context, bm: Bitmap): Uri? {

        val tempFile = context.externalCacheDir?.let { File(it, "temp_${System.currentTimeMillis().toDateAndTimeString()}.png") }
        val bytes = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fileOutPut = FileOutputStream(tempFile)
        fileOutPut.write(bitmapData)
        fileOutPut.flush()
        fileOutPut.close()
        return getUriOfFile(context, tempFile!!)
    }

    fun getUriFromBitmapWithCleanCache(context: Context, bm: Bitmap): Uri? {
        context.externalCacheDir?.listFiles()?.sortedByDescending { it.lastModified() }?.forEachIndexed() { index, file ->
            if (index > 20) {
                file.delete()
            }
        }
        return getUriFromBitmap(context, bm)
    }

    fun getUriOfFile(context: Context, file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriByCopyingFileToPath(context: Context, path: File, fileName: String, uri: Uri): Uri? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUriByCopyingFileToPath(
        context: Context,
        path: File,
        fileName: String,
        file: File
    ): Uri? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            file.copyTo(outputFile, true, DEFAULT_BUFFER_SIZE)
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileByCopyingFileToPath(
        context: Context,
        path: File,
        fileName: String,
        uri: Uri
    ): File? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, uri: Uri): File? {
        return try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, targetFile: File) {
        try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            targetFile.copyTo(outputFile, overwrite = true, DEFAULT_BUFFER_SIZE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveImageToExternalStorage(context: Context, uri: Uri) {
        try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        System.currentTimeMillis().toDateAndTimeString()
                    )
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/Parabox"
                    )
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.also {
                    resolver.openOutputStream(it).use { output ->
                        resolver.openInputStream(uri).use { input ->
                            input?.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                        }
                    }
                }

            } else {
                val path = File(
                    Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_PICTURES}/Parabox"),
                    "${System.currentTimeMillis().toDateAndTimeString()}.jpg"
                )
                path.outputStream().use { output ->
                    resolver.openInputStream(uri).use { input ->
                        input?.copyTo(output, DEFAULT_BUFFER_SIZE)
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun saveImageToExternalStorage(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    System.currentTimeMillis().toDateAndTimeString()
                )
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Parabox"
                )
            }
            val resolver = context.contentResolver
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.also {
                resolver.openOutputStream(it).use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                    }
                }
            }

        } else {
            val path = File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_PICTURES}/Parabox"),
                "${System.currentTimeMillis().toDateAndTimeString()}.jpg"
            )
            path.outputStream().use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }

        }
    }

    fun uriToTempFile(context: Context, uri: Uri) = with(context.contentResolver) {
        val data = readUriBytes(uri) ?: return@with null
        val extension = getUriExtension(uri)
        File(
            context.cacheDir.path,
            "${UUID.randomUUID()}.$extension"
        ).also { audio -> audio.writeBytes(data) }
    }

    fun ContentResolver.readUriBytes(uri: Uri) = openInputStream(uri)
        ?.buffered()?.use { it.readBytes() }

    fun ContentResolver.getUriExtension(uri: Uri) = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(getType(uri))

    fun getSizeString(size: Long): String {
        val format = DecimalFormat("#.##")
        return when (size) {
            in 0 until 1024 -> "${size}B"
            in 1024 until 1048576 -> "${format.format(size.toDouble() / 1024)}KB"
            in 1048576 until 1073741824 -> "${format.format(size.toDouble() / 1048576)}MB"
            else -> "${format.format(size.toDouble() / 1073741824)}GB"
        }
    }

    fun getAvailableFileName(context: Context, acquireName: String, withNumber: Int = 0): String {
        val separatorIndex = acquireName.lastIndexOf('.')
        if(separatorIndex == -1) throw IndexOutOfBoundsException("no separator found in name")
        else{
            val path = File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                acquireName.substringBeforeLast('.') + (if(withNumber == 0) "" else "-${withNumber}") + "." + acquireName.substringAfterLast('.')
            )
            if(!path.exists()) return acquireName
            else return getAvailableFileName(context, acquireName, withNumber + 1)
        }
    }

    fun openFile(context: Context, file: File, extension: String){
        val mineType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
        val uri = getUriOfFile(context, file)
        Intent().apply{
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setAction(Intent.ACTION_VIEW)
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, mineType)
        }.also {
            context.startActivity(it)
        }
    }
}