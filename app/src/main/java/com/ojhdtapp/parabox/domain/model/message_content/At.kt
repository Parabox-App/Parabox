package com.ojhdtapp.parabox.domain.model.message_content

data class At(val target: Long, val name: String) : MessageContent {
    override fun getContentString(): String {
        return "@$name"
    }
}
