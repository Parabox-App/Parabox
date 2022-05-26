package com.ojhdtapp.parabox.domain.model.message_content

data class Image(val bm: ByteArray) : MessageContent{
    override fun getContentString(): String {
        return "[图片]"
    }
}