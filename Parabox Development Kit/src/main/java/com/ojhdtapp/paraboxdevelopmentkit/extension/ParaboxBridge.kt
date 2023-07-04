package com.ojhdtapp.paraboxdevelopmentkit.extension

import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult

interface ParaboxBridge {
    suspend fun receiveMessage(message: ReceiveMessage) : ParaboxResult

    suspend fun recallMessage(uuid: String) : ParaboxResult
}