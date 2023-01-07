package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.util.Log
import com.tencent.cos.xml.CosXmlBaseService
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.cos.xml.CosXmlSimpleService
import com.tencent.cos.xml.exception.CosXmlClientException
import com.tencent.cos.xml.exception.CosXmlServiceException
import com.tencent.cos.xml.listener.CosXmlResultListener
import com.tencent.cos.xml.model.CosXmlRequest
import com.tencent.cos.xml.model.CosXmlResult
import com.tencent.cos.xml.model.PresignedUrlRequest
import com.tencent.cos.xml.model.`object`.GetObjectRequest
import com.tencent.cos.xml.model.`object`.HeadObjectRequest
import com.tencent.cos.xml.model.`object`.PutObjectRequest
import com.tencent.cos.xml.transfer.TransferConfig
import com.tencent.cos.xml.transfer.TransferManager
import com.tencent.qcloud.core.auth.QCloudCredentialProvider
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider
import kotlin.coroutines.suspendCoroutine

object TencentCOSUtil {
    const val SERVICE_CODE = 1002
    fun getCosXmlService(
        context: Context,
        secretId: String,
        secretKey: String,
        region: String
    ): CosXmlSimpleService {
        val credentialProvider = ShortTimeCredentialProvider(secretId, secretKey, 300)
        val serviceConfig = CosXmlServiceConfig.Builder()
            .setRegion(region)
            .isHttps(true)
            .builder()
        return CosXmlSimpleService(context, serviceConfig, credentialProvider)
    }

    suspend fun createFolder(
        context: Context,
        secretId: String,
        secretKey: String,
        region: String,
        bucket: String,
        folder: String
    ): Boolean {
        return suspendCoroutine {
            try {
                val cosXmlService = getCosXmlService(context, secretId, secretKey, region)
                val cosPath = "$folder/"
                val putObjectRequest = PutObjectRequest(bucket, cosPath, byteArrayOf())
                cosXmlService.putObjectAsync(putObjectRequest, object : CosXmlResultListener {
                    override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {
                        it.resumeWith(Result.success(true))
                    }

                    override fun onFail(
                        p0: CosXmlRequest?,
                        p1: CosXmlClientException?,
                        p2: CosXmlServiceException?
                    ) {
                        it.resumeWith(Result.success(false))
                    }
                })
            } catch (e: Exception) {
                it.resumeWith(Result.success(false))
            }
        }
    }

    suspend fun uploadFile(
        context: Context,
        secretId: String,
        secretKey: String,
        region: String,
        bucket: String,
        cosPath: String,
        localPath: String
    ): Boolean {
        return suspendCoroutine {
            try {
                val transferManager = TransferManager(
                    getCosXmlService(context, secretId, secretKey, region),
                    TransferConfig.Builder().build()
                )
                val uploadTask = transferManager.upload(bucket, cosPath, localPath, null)
                uploadTask.setCosXmlResultListener(object : CosXmlResultListener {
                    override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {
                        it.resumeWith(Result.success(true))
                    }

                    override fun onFail(
                        p0: CosXmlRequest?,
                        p1: CosXmlClientException?,
                        p2: CosXmlServiceException?
                    ) {
                        it.resumeWith(Result.success(false))
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                it.resumeWith(Result.success(false))
            }
        }
    }

    suspend fun downloadFile(
        context: Context,
        secretId: String,
        secretKey: String,
        region: String,
        bucket: String,
        cosPath: String,
        localPath: String,
        fileName: String
    ): Boolean {
        return suspendCoroutine {
            try {
                val transferManager = TransferManager(
                    getCosXmlService(context, secretId, secretKey, region),
                    TransferConfig.Builder().build()
                )
                val downloadTask =
                    transferManager.download(context, bucket, cosPath, localPath, fileName)
                downloadTask.setCosXmlResultListener(object : CosXmlResultListener {
                    override fun onSuccess(request: CosXmlRequest?, result: CosXmlResult?) {
                        it.resumeWith(Result.success(true))
                    }

                    override fun onFail(
                        p0: CosXmlRequest?,
                        p1: CosXmlClientException?,
                        p2: CosXmlServiceException?
                    ) {
                        it.resumeWith(Result.success(false))
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
                it.resumeWith(Result.success(false))
            }
        }
    }

    fun getPreSignedDownloadUrl(
        context: Context,
        secretId: String,
        secretKey: String,
        region: String,
        bucket: String,
        cosPath: String
    ): String? {
        return try {
            val cosXmlService = getCosXmlService(context, secretId, secretKey, region)
            val preSignedUrlRequest = PresignedUrlRequest(bucket, cosPath).apply {
                setRequestMethod("GET")
                setSignKeyTime(300)
                addNoSignHeader("Host")
            }
            cosXmlService.getPresignedURL(preSignedUrlRequest)
        } catch( e: Exception) {
            e.printStackTrace()
            null
        }
    }
}