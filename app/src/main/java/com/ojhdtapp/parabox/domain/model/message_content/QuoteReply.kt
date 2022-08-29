package com.ojhdtapp.parabox.domain.model.message_content

import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuoteReply(
    val quoteMessageId: Long?,
    val quoteMessageContent: List<MessageContent>
) : MessageContent {
    @IgnoredOnParcel
    val type = MessageContent.QUOTE_REPLY
    override fun getContentString(): String {
        return "[引用回复]"
    }
}