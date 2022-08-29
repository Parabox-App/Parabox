package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlainText(val text: String) : MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.PLAIN_TEXT
    override fun getContentString(): String {
        return text.trimStart()
    }
}
