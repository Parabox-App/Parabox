package com.ojhdtapp.paraboxdevelopmentkit.model

data class ParaboxResult(
    val code: Int,
    val message: String,
){
    companion object{
        const val SUCCESS = 10000
        const val SUCCESS_MSG = "Success"
        const val ERROR_UNINITIALIZED = 20001
        const val ERROR_UNINITIALIZED_MSG = "Uninitialized"
        const val ERROR_UNKNOWN = 20002
        const val ERROR_UNKNOWN_MSG = "Unknown Error"
        const val ERROR_INVALID_CONTACT_ID = 20003
        const val ERROR_INVALID_CONTACT_ID_MSG = "Invalid Contact ID"
        const val ERROR_INVALID_CHAT_ID = 20004
        const val ERROR_INVALID_CHAT_ID_MSG = "Invalid Chat ID"
        const val ERROR_INVALID_MESSAGE_ID = 20005
        const val ERROR_INVALID_MESSAGE_ID_MSG = "Invalid Message ID"
    }
}
