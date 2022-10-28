package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
data class File(
    val url: String? = null,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModifiedTime: Long,
    val expiryTime: Long? = null,
    val uriString: String? = null
): MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.FILE
    override fun getContentString(): String {
        return "[文件]"
    }
}