package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.Parcelize

@Parcelize
data class At(val target: Long, val name: String) : MessageContent {
    val type = MessageContent.AT
    override fun getContentString(): String {
        return "@$name"
    }
}
