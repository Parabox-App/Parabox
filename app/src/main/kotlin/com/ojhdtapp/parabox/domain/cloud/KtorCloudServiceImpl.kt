package com.ojhdtapp.parabox.domain.cloud

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

// Parts of this file are copied from https://github.com/whitechi73/OpenShamrock/blob/master/xposed/src/main/java/moe/fuqiuluo/shamrock/utils/DownloadUtils.kt
class KtorCloudServiceImpl(
    val context: Context,
    val fileUtil: FileUtil,
    val ktorClient: HttpClient
) : ParaboxCloudService {
    init {
        GlobalScope.launch(Dispatchers.IO) {
            delay(1000)
            Log.d("KtorCloudServiceImpl", "KtorCloudServiceImpl init")
            while (true) {
                if (stateFlowQueue.isNotEmpty()) {
                    val stateFlow = stateFlowQueue.poll()
                    if (stateFlow == null) {
                        delay(1000)
                        continue
                    }
                    if (stateFlow.value is ParaboxCloudStatus.Waiting) {
                        when ((stateFlow.value as ParaboxCloudStatus.Waiting).resourceInfo) {
                            is ParaboxResourceInfo.ParaboxLocalInfo -> {
                                continue
                            }
                            is ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo -> {
                                val remoteResource =
                                    (stateFlow.value as ParaboxCloudStatus.Waiting).resourceInfo as ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo
                                val destFile = FileUtil.createTempFile(
                                    context,
                                    FileUtil.DEFAULT_AUDIO_NAME,
                                    FileUtil.DEFAULT_AUDIO_EXTENSION
                                )
                                val res = download(remoteResource.url, destFile, stateFlow)
                                if (res) {
                                    stateFlow.value = ParaboxCloudStatus.Synced(
                                        localUri = fileUtil.getUriForFile(destFile)!!,
                                        remoteUrl = remoteResource.url
                                    )
                                } else {
                                    stateFlow.value = ParaboxCloudStatus.Failed
                                }
                            }
                            else -> {
                                continue
                            }
                        }
                    } else {
                        continue
                    }
                }
            }
        }
    }
    private val stateFlowQueue = ConcurrentLinkedQueue<MutableStateFlow<ParaboxCloudStatus>>()


    override suspend fun upload(localResource: ParaboxResourceInfo.ParaboxLocalInfo): Flow<ParaboxCloudStatus> {
        return MutableStateFlow(ParaboxCloudStatus.Waiting(localResource))
    }

    override suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo): Flow<ParaboxCloudStatus> {
        return MutableStateFlow(ParaboxCloudStatus.Waiting(remoteResource)).also {
            stateFlowQueue.add(it as MutableStateFlow<ParaboxCloudStatus>)
        }
    }

    suspend fun download(
        urlAdr: String,
        dest: File,
        stateFlow: MutableStateFlow<ParaboxCloudStatus>,
        threadCount: Int = MAX_THREAD,
        headers: Map<String, String> = mapOf()
    ): Boolean {
        return coroutineScope {
            var threadCnt = if(threadCount == 0 || threadCount < 0) MAX_THREAD else threadCount
            val url = URL(urlAdr)
            val connection = withContext(Dispatchers.IO) { url.openConnection() } as HttpURLConnection
            headers.forEach { (k, v) ->
                connection.setRequestProperty(k, v)
            }
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val contentLength = connection.contentLength
                if (contentLength <= 0) {
                    downloadByKtor(url, dest)
                } else {
                    withContext(Dispatchers.IO) {
                        val raf = RandomAccessFile(dest, "rw")
                        raf.setLength(contentLength.toLong())
                        raf.close()
                    }
                }
                if (contentLength <= 1024 * 1024) {
                    threadCnt = 1
                }
                var blockSize = (contentLength * (1.0 / threadCnt)).roundToInt()
                connection.disconnect()
                val progress = atomic(0)
                val channel = Channel<Int>()
                var processed = 0
                repeat(threadCnt) {
                    if (processed + blockSize != contentLength && it == threadCnt - 1) {
                        blockSize = contentLength - processed
                    }
                    val start = processed
                    val end = processed + blockSize - 1
                    GlobalScope.launch(Dispatchers.IO) {
                        reallyDownload(url, start, end, dest, channel)
                    }
                    processed += blockSize
                }
                withTimeoutOrNull(1.minutes) {
                    while (progress.value < contentLength) {
                        stateFlow.value = ParaboxCloudStatus.Downloading(
                            remotePath = urlAdr,
                            progress = progress.value.toFloat() / contentLength,
                            total = contentLength.toLong(),
                            speed = 0
                        )
                        if(progress.addAndGet(channel.receive()) >= contentLength) {
                            break
                        }
                    }
                    return@withTimeoutOrNull true
                } ?: dest.delete()
                true
            }
            false
        }
    }

    private suspend fun downloadByKtor(url: URL, dest: File): Boolean {
        val respond = ktorClient.get(url)
        if (respond.status == HttpStatusCode.OK) {
            val channel = respond.bodyAsChannel()
            withContext(Dispatchers.IO) {
                dest.outputStream().use {
                    channel.toInputStream().use { input ->
                        input.copyTo(it)
                    }
                }
            }
            return true
        } else {
            Log.e("KtorCloudServiceImpl","文件下载失败: ${respond.status}")
        }
        return false
    }

    private suspend fun reallyDownload(url: URL, start: Int, end: Int, dest: File, channel: Channel<Int>) {
        val openConnection: HttpURLConnection = withContext(Dispatchers.IO) { url.openConnection() } as HttpURLConnection
        openConnection.requestMethod = "GET"
        openConnection.connectTimeout = 5000
        openConnection.setRequestProperty("range", "bytes=$start-$end")
        val responseCode = openConnection.responseCode
        if (responseCode == 206) {
            val inputStream = openConnection.inputStream
            val raf = withContext(Dispatchers.IO) {
                RandomAccessFile(dest, "rw").also {
                    it.seek(start.toLong())
                }
            }
            var len: Int
            val buf = ByteArray(1024)
            var flag = true
            while (flag) {
                len = withContext(Dispatchers.IO) {
                    inputStream.read(buf)
                }
                flag = len != -1
                if (flag) {
                    withContext(Dispatchers.IO) {
                        raf.write(buf, 0, len)
                    }
                    channel.send(len)
                }
            }
            withContext(Dispatchers.IO) {
                inputStream.close()
                raf.close()
            }
        }
        openConnection.disconnect()
    }

    companion object {
        private const val MAX_THREAD = 4
    }
}

val LocalCloudService = staticCompositionLocalOf<ParaboxCloudService> {
    error("No cloud service provided")
}