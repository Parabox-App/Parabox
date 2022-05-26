package com.ojhdtapp.parabox.domain.model.message_content

data class PlainText(val text: String) : MessageContent {
    override fun getContentString(): String {
        return text
    }
}
