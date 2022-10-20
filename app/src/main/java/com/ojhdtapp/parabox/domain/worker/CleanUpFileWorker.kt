package com.ojhdtapp.parabox.domain.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ojhdtapp.parabox.domain.use_case.UpdateFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltWorker
class CleanUpFileWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateFile: UpdateFile
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("parabox", "clean up file worker start")
        return withContext(Dispatchers.IO) {
            val fileId = inputData.getLong("fileId", 0L)
            val service = inputData.getInt("service", 0)
            val cloudId = inputData.getString("cloudId")
            val uri = inputData.getString("fileUri")?.let { Uri.parse(it) }
            uri?.path?.let { File(it) }?.delete()
            if (fileId != 0L && service != 0 && cloudId != null) {
                updateFile.cloudInfo(service, cloudId, fileId)
                Result.success()
            } else {
                Result.failure()
            }
        }
    }
}