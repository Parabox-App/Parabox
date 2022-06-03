package com.ojhdtapp.parabox.domain.model.message_content

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class At(val target: Long, val name: String) : MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.AT
    override fun getContentString(): String {
        return "@$name"
    }
}
