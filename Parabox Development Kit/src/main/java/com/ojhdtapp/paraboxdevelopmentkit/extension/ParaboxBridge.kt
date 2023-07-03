package com.ojhdtapp.paraboxdevelopmentkit.extension

import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult

interface ParaboxBridge {
    fun receiveMessage(message: ReceiveMessage) : ParaboxResult

    fun recallMessage(uuid: String) : ParaboxResult
}