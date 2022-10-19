package com.ojhdtapp.parabox.domain.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.use_case.UpdateFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UploadFileWorker(
    val appContext: Context,
    workerParams: WorkerParameters
) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val service = inputData.getInt("default_backup_service", 0)
            when (service) {
                0 -> Result.failure()
                GoogleDriveUtil.SERVICE_CODE -> {
                    val uri = inputData.getString("fileUri")?.let { Uri.parse(it) }
                    val path = uri?.path
                    val folderId =
                        appContext.dataStore.data.first()[DataStoreKeys.GOOGLE_WORK_FOLDER_ID]
                    val fileName = inputData.getString("name") ?: "File"
                    if (path == null || folderId == null) return@withContext Result.failure()
                    val cloudId = GoogleDriveUtil.uploadFile(appContext, folderId, fileName, path)
                    if (cloudId != null) {
                        Result.success(
                            workDataOf(
                                "cloudId" to cloudId,
                                "service" to service,
                                "fileUri" to uri.toString()
                            )
                        )
                    } else {
                        Result.failure()
                    }
                }
                else -> Result.failure()
            }
        }
    }
}