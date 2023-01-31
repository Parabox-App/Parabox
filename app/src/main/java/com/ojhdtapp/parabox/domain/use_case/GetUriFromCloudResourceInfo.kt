package com.ojhdtapp.parabox.domain.use_case

import android.content.Context
import android.net.Uri
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetUriFromCloudResourceInfo @Inject constructor(
    val context: Context,
    private val downloadUtil: DownloadUtil
) {
    suspend operator fun invoke(
        fileName: String,
        cloudType: Int,
        url: String?,
        cloudId: String?
    ): Uri?{
        return when (cloudType) {
            FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> {
                cloudId?.let {
                    GoogleDriveUtil.downloadFile(
                        context,
                        it,
                        context.externalCacheDir!!
                    )
                }?.let {
                    FileUtil.getUriOfFile(context, it)
                }
            }

            FcmConstants.CloudStorage.TENCENT_COS.ordinal -> {
                cloudId?.let { cosPath ->
                    val secretId =
                        context.dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_ID]
                    val secretKey =
                        context.dataStore.data.first()[DataStoreKeys.TENCENT_COS_SECRET_KEY]
                    val bucket =
                        context.dataStore.data.first()[DataStoreKeys.TENCENT_COS_BUCKET]
                    val region =
                        context.dataStore.data.first()[DataStoreKeys.TENCENT_COS_REGION]
                    if (secretId != null && secretKey != null && bucket != null && region != null) {
                        val res = TencentCOSUtil.downloadFile(
                            context,
                            secretId,
                            secretKey,
                            region,
                            bucket,
                            cosPath,
                            context.externalCacheDir!!.absolutePath,
                            fileName
                        )
                        if (res) {
                            FileUtil.getUriOfFile(
                                context,
                                java.io.File(context.externalCacheDir!!, fileName)
                            )
//                            val file = context.getExternalFilesDir("chat")!!.listFiles { file ->
//                                file.name == fileName
//                            }?.firstOrNull()
//                            if (file != null) {
//                                FileUtil.getUriOfFile(
//                                    context,
//                                    file
//                                )
//                            } else null
                        } else null
                    } else null
                } ?: url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        context.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(context, it)
                    }
                }
            }

            FcmConstants.CloudStorage.QINIU_KODO.ordinal -> {
                cloudId?.let { key ->
                    val accessKey =
                        context.dataStore.data.first()[DataStoreKeys.QINIU_KODO_ACCESS_KEY]
                    val secretKey =
                        context.dataStore.data.first()[DataStoreKeys.QINIU_KODO_SECRET_KEY]
                    val bucket =
                        context.dataStore.data.first()[DataStoreKeys.QINIU_KODO_BUCKET]
                    val domain =
                        context.dataStore.data.first()[DataStoreKeys.QINIU_KODO_DOMAIN]
                    if (accessKey != null && secretKey != null && bucket != null && domain != null) {
                        QiniuKODOUtil.downloadFile(domain, accessKey, secretKey, key)?.let{ newUrl ->
                            downloadUtil.downloadUrl(
                                newUrl,
                                fileName,
                                context.externalCacheDir!!
                            )?.let {
                                FileUtil.getUriOfFile(context, it)
                            }
                        }
                    } else null
                } ?: url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        context.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(context, it)
                    }
                }
            }

            else -> {
                url?.let { url ->
                    downloadUtil.downloadUrl(
                        url,
                        fileName,
                        context.externalCacheDir!!
                    )?.let {
                        FileUtil.getUriOfFile(context, it)
                    }
                }
            }
        }
    }
}