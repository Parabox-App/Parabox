package com.ojhdtapp.parabox.domain.model.message_content

import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
object AtAll: MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.AT_ALL
    override fun getContentString(): String {
        return "全体成员"
    }
}