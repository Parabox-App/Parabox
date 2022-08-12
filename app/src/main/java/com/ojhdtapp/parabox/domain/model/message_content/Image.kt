package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(val url: String, val width: Int, val height: Int) : MessageContent{
    val type = MessageContent.IMAGE
    override fun getContentString(): String {
        return "[图片]".trimStart()
    }
}