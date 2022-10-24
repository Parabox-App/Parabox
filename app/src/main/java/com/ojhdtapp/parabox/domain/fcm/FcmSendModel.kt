package com.ojhdtapp.parabox.domain.fcm

import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto

data class FcmSendModel(
    val sendMessageDto: SendMessageDto,
    val loopbackToken: String
)
