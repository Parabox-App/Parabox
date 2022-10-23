package com.ojhdtapp.parabox.domain.fcm

object FcmConstants {
    sealed interface Status{
        data class Success(val version: String) : Status
        object Failure : Status
        object Loading : Status
    }
}