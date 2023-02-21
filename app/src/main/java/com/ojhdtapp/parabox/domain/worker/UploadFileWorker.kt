package com.ojhdtapp.parabox.domain.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.OnedriveUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.use_case.UpdateFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltWorker
class UploadFileWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    val onedriveUtil: OnedriveUtil
) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("parabox", "upload file worker start")
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
                OnedriveUtil.SERVICE_CODE -> {
                    val uri = inputData.getString("fileUri")?.let { Uri.parse(it) }
                    val path = uri?.path
                    val fileName = inputData.getString("name")
                    if (path == null || fileName == null) return@withContext Result.failure()
                    val res = onedriveUtil.uploadFile(appContext, File(path))
                    if (res != null) {
                        Result.success(
                            workDataOf(
                                "cloudId" to res.id,
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