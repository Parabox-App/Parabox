package com.ojhdtapp.parabox.domain.fcm

object FcmConstants {
    sealed interface Status {
        data class Success(val version: String) : Status
        object Failure : Status
        object Loading : Status
    }

    enum class CloudStorage{
        NONE,
        GOOGLE_DRIVE,
        TENCENT_COS,
        QINIU_KODO
    }

    const val CONNECTION_TYPE = 9999
}