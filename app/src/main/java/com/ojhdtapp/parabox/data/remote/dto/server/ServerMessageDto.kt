package com.ojhdtapp.parabox.data.remote.dto.server

import com.ojhdtapp.parabox.data.remote.dto.server.content.Content

data class ServerMessageDto(
    val chatType: Int,
    val contents: List<Content>,
    val profile: Profile,
    val slaveMsgId: String,
    val slaveOriginUid: String,
    val subjectProfile: Profile,
    val timestamp: Long
)