package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.Parcelize

@Parcelize
data class Audio(val url: String, val length: Long, val fileName: String, val fileSize: Long) : MessageContent{
    val type = MessageContent.AUDIO
    override fun getContentString(): String {
        return "[语音]"
    }
}
