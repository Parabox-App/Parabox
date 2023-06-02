package com.ojhdtapp.paraboxdevelopmentkit.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import kotlinx.parcelize.Parcelize

@Parcelize
data class SendMessage(
    val contents: List<ParaboxMessageElement>,
    val contact: ParaboxContact,
    val timestamp: Long,
    val uuid: String,
) : Parcelable
