package com.ojhdtapp.parabox.domain.model.message_content

import android.os.Parcelable

interface MessageContent : Parcelable {
    companion object{
        const val PLAIN_TEXT = 0
        const val IMAGE = 1
        const val AT = 2
        const val AUDIO = 3
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