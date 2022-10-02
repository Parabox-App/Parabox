package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatestMessage(
    val sender: String,
    val content: String,
    val timestamp: Long,
    val unreadMessagesNum: Int = 0,
    val sentByMe: Boolean = false
): Parcelable
