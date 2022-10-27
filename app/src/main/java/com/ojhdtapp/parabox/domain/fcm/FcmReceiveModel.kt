package com.ojhdtapp.parabox.domain.fcm

import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto

data class FcmReceiveModel(
    val receiveMessageDto: ReceiveMessageDto,
    val notification: FcmNotification,
    val targetTokensSet: Set<String>
)
