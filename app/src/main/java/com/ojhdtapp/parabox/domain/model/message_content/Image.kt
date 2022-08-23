package com.ojhdtapp.parabox.domain.model.message_content

import android.net.Uri
import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(val url: String?, val width: Int, val height: Int, val uriString: String?) : MessageContent{
    val type = MessageContent.IMAGE
    override fun getContentString(): String {
        return "[图片]".trimStart()
    }
}