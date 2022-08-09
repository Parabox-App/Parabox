package com.ojhdtapp.parabox.domain.service

object ConnKey {
    const val SUCCESS = 1
    const val FAILURE = 0

    const val MSG_MESSAGE = 0
    const val MSG_COMMAND = 1
    const val MSG_RESPONSE = 2

    const val MSG_COMMAND_START_SERVICE = 10
    const val MSG_COMMAND_STOP_SERVICE = 11
    const val MSG_COMMAND_LOGIN = 12
    const val MSG_COMMAND_SUBMIT_VERIFICATION_RESULT = 13
    const val MSG_COMMAND_ON_LOGIN_STATE_CHANGED = 14

    const val MSG_RESPONSE_START_SERVICE = 20
    const val MSG_RESPONSE_STOP_SERVICE = 21
    const val MSG_RESPONSE_LOGIN = 22
    const val MSG_RESPONSE_SUBMIT_VERIFICATION_RESULT = 23

    const val MSG_MESSAGE_CHECK_RUNNING_STATUS = 30

    const val MSG_RESPONSE_CHECK_RUNNING_STATUS = 40
}