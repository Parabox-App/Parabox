package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ojhdtapp.parabox.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

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

    fun getUriOfFile(context: Context, file: File): Uri?{
        return try{
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider", file
            )
        }catch (e: Exception){
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
                    inputStream.copyTo(outputStream)
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
                    inputStream.copyTo(outputStream)
                }
            }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, uri: Uri){
        try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyFileToPath(context: Context, path: File, fileName: String, targetFile: File){
        try {
            if (!path.exists()) path.mkdirs()
            val outputFile = File(path, fileName)
            targetFile.copyTo(outputFile, overwrite = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSizeString(size:Long):String{
        val format = DecimalFormat("#.##")
        return when(size){
            in 0 until 1024 -> "${size}B"
            in 1024 until 1048576 -> "${format.format(size.toDouble()/1024)}KB"
            in 1048576 until 1073741824 -> "${format.format(size.toDouble()/1048576)}MB"
            else -> "${format.format(size.toDouble()/1073741824)}GB"
        }
    }
}