package com.ojhdtapp.parabox.core.util

import com.google.gson.Gson
import com.qiniu.storage.Configuration
import com.qiniu.storage.DownloadUrl
import com.qiniu.storage.Region
import com.qiniu.storage.UploadManager
import com.qiniu.storage.model.DefaultPutRet
import com.qiniu.util.Auth
import kotlin.coroutines.suspendCoroutine

object QiniuKODOUtil {

    fun uploadFile(
        accessKey: String,
        secretKey: String,
        bucket: String,
        fileName: String,
        localPath: String
    ): String? {
        return try {
            val auth = Auth.create(accessKey, secretKey)
            val token = auth.uploadToken(bucket, fileName)
            val cfg = Configuration(Region.autoRegion())
            val uploadManager = UploadManager(cfg)
            val response = uploadManager.put(localPath, fileName, token)
            val putRet = Gson().fromJson<DefaultPutRet>(response.bodyString(), DefaultPutRet::class.java)
            putRet.key
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadFile(
        domain: String,
        accessKey: String,
        secretKey: String,
        key: String
    ): String? {
        return try {
            val baseUrl = DownloadUrl(domain, false, key).buildURL()
            val auth = Auth.create(accessKey, secretKey)
            val privateUrl = auth.privateDownloadUrl(baseUrl)
            privateUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getFileSize(
        accessKey: String,
        secretKey: String,
        bucket: String,
        key: String
    ): Long? {
        return suspendCoroutine {
            try {
                val auth = Auth.create(accessKey, secretKey)
                val bucketManager = com.qiniu.storage.BucketManager(auth, Configuration(Region.autoRegion()))
                val fileInfo = bucketManager.stat(bucket, key)
                it.resumeWith(Result.success(fileInfo.fsize))
            } catch (e: Exception) {
                e.printStackTrace()
                it.resumeWith(Result.success(null))
            }
        }
    }
}