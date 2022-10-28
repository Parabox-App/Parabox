package com.ojhdtapp.parabox.domain.model.message_content

import android.net.Uri
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(val url: String?, val width: Int, val height: Int, val fileName: String, val uriString: String?) : MessageContent{
    @IgnoredOnParcel
    val type = MessageContent.IMAGE
    override fun getContentString(): String {
        return "[图片]".trimStart()
    }
}