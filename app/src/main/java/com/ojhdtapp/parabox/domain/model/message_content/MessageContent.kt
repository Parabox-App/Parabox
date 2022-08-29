package com.ojhdtapp.parabox.domain.model.message_content

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
    }
    fun getContentString() : String
}

fun List<MessageContent>.getContentString() : String{
    val builder = StringBuilder()
    forEach {
        builder.append(it.getContentString())
    }
    return builder.toString()
}