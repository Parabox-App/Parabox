package com.ojhdtapp.parabox.domain.model.message_content

import android.net.Uri
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Audio(val url: String?, val length: Long, val fileName: String, val fileSize: Long, val uriString: String?) : MessageContent{
    @IgnoredOnParcel
    val type = MessageContent.AUDIO
    override fun getContentString(): String {
        return "[语音]"
    }
}
