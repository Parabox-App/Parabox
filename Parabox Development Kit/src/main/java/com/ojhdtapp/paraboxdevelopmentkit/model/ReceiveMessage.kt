package com.ojhdtapp.paraboxdevelopmentkit.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReceiveMessage(
    val contents: List<ParaboxMessageElement>,
    val sender: ParaboxContact,
    val chat: ParaboxChat,
    val timestamp: Long,
    val uuid: String,
) : Parcelable
