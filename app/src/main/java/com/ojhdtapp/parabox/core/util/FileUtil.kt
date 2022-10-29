package com.ojhdtapp.parabox.core.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.*

object FileUtil {

    data class CloudResourceInfo(
        val cloudType: Int,
        val url: String? = null,
        val cloudId: String? = null,
    )

    suspend fun getCloudResourceInfoWithSelectedCloudStorage(
        context: Context,
        fileName: String,
        filePath: String
    ): CloudResourceInfo? =
        coroutineScope {
            context.dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE]?.let { cloudStorage ->
                when (cloudStorage) {
                    FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> {
                        val folderId =
                            GoogleDriveUtil.getFolderId(context, "ParaboxTemp")
                                ?: GoogleDriveUtil.createFolder(context, "ParaboxTemp")
                        folderId?.let {
                            val cloudId =
                                GoogleDriveUtil.uploadFile(context, it, fileName, filePath)
                            CloudResourceInfo(
                                cloudType = FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal,
                                url = null,
                                cloudId = cloudId
                            )
                        }
                    }

                    else -> null
                }
            }
        }

    suspend fun getCloudResourceInfoWithSelectedCloudStorage(
        context: Context,
        fileName: String,
        fileUri: Uri,
    ): CloudResourceInfo? {
        return try {
            val inputPFD: ParcelFileDescriptor? =
                context.contentResolver.openFileDescriptor(fileUri, "r")
            val fd = inputPFD!!.fileDescriptor
            val inputStream = FileInputStream(fd)
            val tempFile = File.createTempFile(fileName, "", context.externalCacheDir)
            inputStream.use {
                org.apache.commons.io.FileUtils.copyInputStreamToFile(it, tempFile)
            }
            getCloudResourceInfoWithSelectedCloudStorage(context, fileName, tempFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile =
            File(targetDir, "temp_${System.currentTimeMillis().toDateAndTimeString()}.png")
        val bytes = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val bitmapData = bytes.toByteArray()

        val fileOutPut = FileOutputStream(tempFile)
        fileOutPut.write(bitmapData)
        fileOutPut.flush()
        fileOutPut.close()
        return getUriOfFile(context, tempFile)
    }

    fun getUriFromBitmapWithCleanCache(context: Context, bm: Bitmap): Uri? {
        val targetDir = File(context.externalCacheDir, "bm")
        if (!targetDir.exists()) targetDir.mkdirs()
        targetDir.listFiles()?.sortedByDescending { it.lastModified() }
            ?.forEachIndexed() { index, file ->
                if (index > 20) {
                    file.delete()
                }
            }
        return getUriFromBitmap(context, bm)
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
        file: File,
        path: File,
    ): Uri? {
        return try {
            file.inputStream().use { ips ->
                path.outputStream().use { ops ->
                    ips.copyTo(ops, DEFAULT_BUFFER_SIZE)
                }
            }
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", path
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
        return when (size.coerceAtLeast(0)) {
            in 0 until 1024 -> "${size}B"
            in 1024 until 1048576 -> "${format.format(size.toDouble() / 1024)}KB"
            in 1048576 until 1073741824 -> "${format.format(size.toDouble() / 1048576)}MB"
            else -> "${format.format(size.toDouble() / 1073741824)}GB"
        }
    }

    fun getAvailableFileName(context: Context, acquireName: String, withNumber: Int = 0): String {
        val separatorIndex = acquireName.lastIndexOf('.')
        if (separatorIndex == -1) throw IndexOutOfBoundsException("no separator found in name")
        else {
            val path = File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                acquireName.substringBeforeLast('.') + (if (withNumber == 0) "" else "-${withNumber}") + "." + acquireName.substringAfterLast(
                    '.'
                )
            )
            if (!path.exists()) return acquireName
            else return getAvailableFileName(context, acquireName, withNumber + 1)
        }
    }

    fun openFile(context: Context, file: File, extension: String) {
        val mineType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
        val uri = getUriOfFile(context, file)
        Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setAction(Intent.ACTION_VIEW)
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(uri, mineType)
        }.also {
            context.startActivity(it)
        }
    }

    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "*/*")
    }

    fun getFileName(context: Context, uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(context, uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun getContentFileName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                .let(cursor::getString)
        }
    }.getOrNull()
}