package com.ojhdtapp.paraboxdevelopmentkit.model.res_info

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReceivePureMessage(
    val contents: List<ParaboxMessageElement>,
    val senderId: String,
    val chatId: String,
    val timestamp: Long,
    val uuid: String,
) : Parcelable
