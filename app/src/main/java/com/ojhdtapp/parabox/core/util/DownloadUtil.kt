package com.ojhdtapp.parabox.core.util

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class DownloadUtil @Inject constructor(
    val context: Context,
    val downloadUtilService: DownloadUtilService
) {

    suspend fun downloadUrl(url: String, fileName: String, savePath: File): File? =
        coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    val responseBody = downloadUtilService.downloadUrl(url).body()
                    FileOutputStream(
                        File(savePath, fileName)
                    ).use { output ->
                        responseBody?.byteStream().use { input ->
                            input?.copyTo(output, DEFAULT_BUFFER_SIZE)
                        }
                    } ?: throw RuntimeException("failed to download: $url")
                    savePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
}

interface DownloadUtilService {
    @GET
    @Streaming
    suspend fun downloadUrl(@Url utl: String): Response<ResponseBody>
}
