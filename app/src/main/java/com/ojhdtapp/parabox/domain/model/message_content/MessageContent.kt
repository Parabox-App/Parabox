package com.ojhdtapp.parabox.domain.model.message_content

import android.net.Uri
import android.os.Parcelable

interface MessageContent : Parcelable {
    companion object{
        const val PLAIN_TEXT = 0
        const val IMAGE = 1
        const val AT = 2
        const val AUDIO = 3
        const val QUOTE_REPLY = 4
        const val AT_ALL = 5
        const val FILE = 6
        const val LOCATION = 7
    }
    fun getContentString() : String
}

fun List<MessageContent>.getContentString(): String {
    val builder = StringBuilder()
    forEachIndexed { index, messageContent ->
        builder.append(messageContent.getContentString())
        if (index != lastIndex) {
            builder.append(" ")
        }
    }
    return builder.toString()
}

fun MessageContent.toMessageContent():com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent{
    return when(this){
        is PlainText -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(text)
        is Image -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image(
            url, width, height, fileName, uriString?.let { Uri.parse(it) }
        )
        is AtAll -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.AtAll
        is Audio -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Audio(
            url, length, fileName, fileSize, uriString?.let { Uri.parse(it) }
        )
        is QuoteReply -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.QuoteReply(
            quoteMessageSenderName, quoteMessageTimestamp, quoteMessageId, quoteMessageContent?.toMessageContentList()
        )
        is File -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File(
            url, name, extension, size, lastModifiedTime, expiryTime, uriString?.let{ Uri.parse(it)}
        )
        else -> com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(getContentString())
    }
}

fun List<MessageContent>.toMessageContentList() : List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>{
    return this.map {
        it.toMessageContent()
    }
}