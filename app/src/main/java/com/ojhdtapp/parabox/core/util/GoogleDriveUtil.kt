package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import kotlinx.coroutines.*
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

    suspend fun getFileName(
        context: Context,
        fileId: String
    ): String? {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val result = driveService.files().get(fileId)
                            .setFields("name")
                            .execute()
                        result.name
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
        path: File,
//        listener: MediaHttpDownloaderProgressListener? = null,
        onProgress: (downloadedBytes: Long, allBytes: Long) -> Unit,
        fileName: String? = null,
    ): File? {
        return coroutineScope {
            if (!path.exists()) {
                path.mkdirs()
            }
            withContext(Dispatchers.IO) {
                try {
                    getDriveService(context)?.let { driveService ->
                        val listener =
                            MediaHttpDownloaderProgressListener { downloader ->
                                if (downloader != null) {
                                    when (downloader.downloadState) {
                                        MediaHttpDownloader.DownloadState.MEDIA_IN_PROGRESS -> {
                                            onProgress(
                                                downloader.numBytesDownloaded,
                                                (downloader.numBytesDownloaded / downloader.progress).toLong()
                                            )
                                        }
                                        MediaHttpDownloader.DownloadState.MEDIA_COMPLETE -> {
                                            onProgress(
                                                downloader.numBytesDownloaded,
                                                downloader.numBytesDownloaded
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        val request = driveService.files().get(fileId).apply {
                            mediaHttpDownloader.progressListener = listener
                            mediaHttpDownloader.chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
                        }
//                        launch {
//                            while (request.mediaHttpDownloader.downloadState != MediaHttpDownloader.DownloadState.MEDIA_COMPLETE) {
//                                delay(1000)
//                                Log.d("GoogleDrive", "downloadFile: ${request.mediaHttpDownloader.numBytesDownloaded} / ${request.mediaHttpDownloader.progress}")
//                                onProgress(
//                                    request.mediaHttpDownloader.numBytesDownloaded,
//                                    (request.mediaHttpDownloader.numBytesDownloaded / request.mediaHttpDownloader.progress).toLong()
//                                )
//                            }
//                        }
                        val finalFilename = fileName ?: request.execute().name
                        val targetFile =
                            File(path, FileUtil.getAvailableFileName(context, finalFilename))
                        targetFile.outputStream().use { fos ->
                            request.executeMediaAndDownloadTo(fos)
                        }
                        targetFile
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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