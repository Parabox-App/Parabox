package com.ojhdtapp.parabox.domain.fcm

import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString

data class FcmNotification(
    val title: String,
    val body: String,
    val image: String? = null
)

fun ReceiveMessageDto.getFcmNotification(): FcmNotification {
    val imageUrl = contents.filterIsInstance<Image>().firstOrNull()?.url
    return FcmNotification(
        title = subjectProfile.name,
        body = "${if (profile.name != subjectProfile.name) profile.name + ":" else ""}${contents.getContentString()}",
        image = imageUrl
    )
}
