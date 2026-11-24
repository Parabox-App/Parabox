package com.ojhdtapp.paraboxdevelopmentkit.model

data class ParaboxResult(
    val code: Int,
    val message: String,
    val messageUuid: String? = null,
){
    companion object{
        const val SUCCESS = 10000
        const val SUCCESS_MSG = "Success"
        const val ERROR_UNINITIALIZED = 20001
        const val ERROR_UNINITIALIZED_MSG = "Uninitialized"
        const val ERROR_UNKNOWN = 20002
        const val ERROR_UNKNOWN_MSG = "Unknown Error"
        const val ERROR_TIMEOUT = 20003
        const val ERROR_TIMEOUT_MSG = "Request Timeout"
        const val ERROR_DISCONNECTED = 20004
        const val ERROR_DISCONNECTED_MSG = "Disconnected"
        const val ERROR_UNSUPPORTED = 20005
        const val ERROR_UNSUPPORTED_MSG = "Unsupported"
        const val ERROR_MEDIA_UNREADABLE = 20006
        const val ERROR_MEDIA_UNREADABLE_MSG = "Media Unreadable"
    }
}
