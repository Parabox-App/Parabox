package com.ojhdtapp.parabox.core.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileUtil @Inject constructor(
    @ApplicationContext val context: Context
) {
    fun getFileNameExtension(file: Any): String? {
        return getFileName(file)?.substringAfterLast('.', "*/*")
    }

    fun getFileName(file: Any): String? {
        return when (file) {
            is Uri -> getUriFileName(file)
            is File -> file.name
            else -> null
        }
    }

    private fun getUriFileName(uri: Uri): String? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    cursor.moveToFirst()
                    return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        .let(cursor::getString)
                }
            }.getOrNull()

            else -> uri.path?.let(::File)?.name
        }
    }

    fun openInputStream(file: Any): InputStream? {
        return when (file) {
            is Uri -> context.contentResolver.openInputStream(file)
            is File -> FileUtils.openInputStream(file)
            else -> null
        }
    }

    fun createPathOnExternalFilesDir(root: String, name: String): File {
        val path = File(context.getExternalFilesDir(root), name)
        return path
    }

    fun deleteFileOnExternalFilesDir(root: String, name: String): Boolean {
        val path = File(context.getExternalFilesDir(root), name)
        return if (path.exists()) {
            path.delete()
        } else {
            false
        }
    }

    fun copyFileToPath(file: Any, path: File): Boolean {
        return when (file) {
            is Uri -> copyFileToPath(file, path)
            is File -> copyFileToPath(file, path)
            else -> false
        }
    }

    private fun copyFileToPath(uri: Uri, path: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileUtils.copyInputStreamToFile(inputStream, path)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyFileToPath(file: File, path: File): Boolean {
        return try {
            FileUtils.copyFile(file, path)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getFileNameWithoutExtension(file: Any): String? {
        return getFileName(file)?.substringBeforeLast('.', "*/*")
    }

    fun saveImageToExternalStorage(file: Any): Boolean {
        val fileName = getFileName(file)
        val extension = getFileNameExtension(file)
        if (fileName == null || extension == null) return false
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                        ?: "image/png")
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
                    openInputStream(file).use { input ->
                        input?.copyTo(output!!, DEFAULT_BUFFER_SIZE) ?: return false
                    }
                }
            }

        } else {
            val path = File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_PICTURES}/Parabox"),
                fileName
            )
            FileUtils.openOutputStream(path).use { output ->
                openInputStream(file).use { input ->
                    input?.copyTo(output, DEFAULT_BUFFER_SIZE) ?: return false
                }
            }
        }
        return true
    }

    fun getUriForFile(file: File): Uri? {
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

    companion object {
        const val EXTERNAL_FILES_DIR_MEME = "meme"
        const val EXTERNAL_FILES_DIR_CAMERA = "camera"
        const val DEFAULT_IMAGE_EXTENSION = "jpg"
    }
}