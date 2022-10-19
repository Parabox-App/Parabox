package com.ojhdtapp.parabox.domain.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ojhdtapp.parabox.core.util.DownloadManagerUtil
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadFileWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val url = inputData.getString("url")
            val fileName = inputData.getString("name")
            if (url == null || fileName == null) return@withContext Result.retry()
            val tempFile = appContext.externalCacheDir?.let { File(it, fileName) }
            val id = DownloadManagerUtil.downloadWithManagerToUri(
                appContext,
                url,
                Uri.fromFile(tempFile)
            )
                ?: return@withContext Result.retry()
            if (DownloadManagerUtil.retrieveResultOnly(appContext, id)) {
                Result.success(workDataOf(
                    "fileUri" to Uri.fromFile(tempFile).toString(),
                    "name" to fileName
                ))
            } else {
                Result.retry()
            }
        }
    }
}