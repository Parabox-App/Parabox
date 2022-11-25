package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.ojhdtapp.parabox.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

object GoogleDriveUtil {
    const val SERVICE_CODE = 1001
    fun isUserSignedIn(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null

    }

    fun getDriveService(context: Context): Drive? {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(context.getString(R.string.app_name))
                .build()
        }
        return null
    }

    suspend fun getDriveInformation(context: Context): GoogleDriveInformation? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val about = driveService.about().get().setFields("storageQuota").execute()
                        val storageQuota = about.storageQuota
                        val totalSpace = storageQuota.limit
                        val usedSpace = storageQuota.usage
                        val freeSpace = totalSpace - usedSpace

                        val folderId =
                            getFolderId(context, "Parabox") ?: createFolder(context, "Parabox")
                        val appUsedSpace = getFolderSize(context, folderId!!)
                        GoogleDriveInformation(
                            folderId,
                            totalSpace,
                            usedSpace,
                            appUsedSpace
                        )
                    }
                } catch (e: SocketException) {
                    null
                } catch (e: IOException) {
                    null
                }
            }
        }
    }

    suspend fun createFolder(context: Context, folderName: String): String? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                getDriveService(context)?.let { driveService ->
                    val fileMetadata = com.google.api.services.drive.model.File()
                    fileMetadata.name = folderName
                    fileMetadata.mimeType = "application/vnd.google-apps.folder"
                    val file = driveService.files().create(fileMetadata)
                        .setFields("id")
                        .execute()
                    file.id
                }
            }
        }
    }

    suspend fun isFolderExist(context: Context, folderName: String): Boolean {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                getDriveService(context)?.let { driveService ->
                    val result = driveService.files().list()
                        .setQ("name='$folderName' and mimeType='application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .execute()
                    result.files.isNotEmpty()
                } ?: false
            }
        }
    }

    suspend fun getFolderId(context: Context, folderName: String): String? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                getDriveService(context)?.let { driveService ->
                    val result = driveService.files().list()
                        .setQ("name='$folderName' and mimeType='application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .execute()
                    result.files.firstOrNull()?.id
                }
            }
        }
    }

    suspend fun getFolderSize(context: Context, folderId: String): Long {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                getDriveService(context)?.let { driveService ->
                    val result = driveService.files().list()
                        .setQ("'$folderId' in parents")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, size)")
                        .execute()
                    var size = 0L
                    result.files.forEach {
                        size += it.getSize()
                    }
                    size
                } ?: 0L
            }
        }
    }

    suspend fun uploadFile(
        context: Context,
        folderId: String,
        fileName: String,
        filePath: String
    ): String? {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    val minetype = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(FileUtil.getExtension(fileName))
                    getDriveService(context)?.let { driveService ->
                        val fileMetadata = com.google.api.services.drive.model.File()
                        fileMetadata.name = fileName
                        fileMetadata.parents = listOf(folderId)
                        val mediaContent =
                            com.google.api.client.http.FileContent(
                                minetype,
                                java.io.File(filePath)
                            )
                        val file = driveService.files().create(fileMetadata, mediaContent)
                            .execute()
                        file.id
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    suspend fun getFileList(
        context: Context,
        folderId: String
    ): List<com.google.api.services.drive.model.File>? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val result = driveService.files().list()
                            .setQ("'$folderId' in parents")
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, size, fullFileExtension, createdTime, webContentLink)")
                            .execute()
                        result.files
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    suspend fun getContentUrl(context: Context, fileId: String): String? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val result = driveService.files().get(fileId)
                            .setFields("webContentLink")
                            .execute()
                        result.webContentLink
                    }
                } catch (e: SocketTimeoutException) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    suspend fun downloadFile(
        context: Context,
        fileId: String,
        path: File
    ): File? {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val file = driveService.files().get(fileId).execute()
                        Looper.prepare()
                        Toast.makeText(context, "开始下载${file.name}", Toast.LENGTH_SHORT)
                            .show()
                        val targetFile = File(path, FileUtil.getAvailableFileName(context, file.name))
                        targetFile.outputStream().use { fos ->
                            driveService.files().get(fileId).executeMediaAndDownloadTo(fos)
                        }
                        targetFile
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Looper.prepare()
                    Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT)
                        .show()
                    null
                }
            }
        }
    }
}

data class GoogleDriveInformation(
    var workFolderId: String,
    var totalSpace: Long = 0,
    var usedSpace: Long = 0,
    var appUsedSpace: Long = 0
)