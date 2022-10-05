package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.util.Log
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

object GoogleDriveUtil {
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

    suspend fun getDriveInformation(context: Context) : GoogleDriveInformation? {
        return coroutineScope {
            withContext(Dispatchers.Default) {
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
                        size += it.size
                    }
                    size
                } ?: 0L
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