package com.ojhdtapp.parabox.domain.model.chat

data class TimeBlock(
    val timeStamp: Long,
    var chats: List<ChatBlock>
)