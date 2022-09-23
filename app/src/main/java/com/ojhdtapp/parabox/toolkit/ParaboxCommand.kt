package com.ojhdtapp.parabox.toolkit

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object ParaboxKey {
    const val CLIENT_MAIN_APP = 0
    const val CLIENT_CONTROLLER = 1
    const val CLIENT_SERVICE = 2

    const val TYPE_COMMAND = 9
    const val TYPE_NOTIFICATION = 8
    const val TYPE_REQUEST = 7

    const val COMMAND_START_SERVICE = 10
    const val COMMAND_STOP_SERVICE = 11
    const val COMMAND_FORCE_STOP_SERVICE = 12
    const val COMMAND_SEND_MESSAGE = 13
    const val COMMAND_RECALL_MESSAGE = 14
    const val COMMAND_GET_UNRECEIVED_MESSAGE = 15

    const val NOTIFICATION_STATE_UPDATE = 20

    const val REQUEST_RECEIVE_MESSAGE = 30

    const val ERROR_TIMEOUT = 60
    const val ERROR_DISCONNECTED = 61
    const val ERROR_REPEATED_CALL = 62
    const val ERROR_RESOURCE_NOT_FOUND = 63
    const val ERROR_SEND_FAILED = 63

    const val STATE_STOP = 70
    const val STATE_PAUSE = 71
    const val STATE_ERROR = 72
    const val STATE_LOADING = 73
    const val STATE_RUNNING = 74
}

@Parcelize
sealed class ParaboxResult(
    open val command: Int,
    open val timestamp: Long,
) : Parcelable {
    data class Success(
        override val command: Int,
        override val timestamp: Long,
        val obj : Bundle = Bundle()
    ) : ParaboxResult(command = command, timestamp = timestamp)

    data class Fail(
        override val command: Int,
        override val timestamp: Long,
        val errorCode: Int,
    ) : ParaboxResult(command = command, timestamp = timestamp)
}