package com.ojhdtapp.parabox.domain.model.message_content

import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
data class File(
    val url: String? = null,
    val name: String,
    val size: Long,
    val lastModifiedTime: Long,
    val expiryTime: Long? = null,
    val uri: String? = null
): MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.FILE
    override fun getContentString(): String {
        return "[文件]"
    }
}