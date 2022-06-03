package com.ojhdtapp.parabox.domain.model

data class LatestMessage(
    val content: String,
    val timestamp: Long,
    val unreadMessagesNum: Int = 0,
)
