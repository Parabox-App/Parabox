package com.ojhdtapp.parabox_dto

import android.os.Parcelable
import com.ojhdtapp.parabox_dto.message_content.MessageContent

import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDto(
    val contents: List<MessageContent>,
    val profile: Profile,
    val subjectProfile: Profile,
    val timestamp: Long,
    val pluginConnection: PluginConnection
) : Parcelable