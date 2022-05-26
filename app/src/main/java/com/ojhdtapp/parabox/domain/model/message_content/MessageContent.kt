package com.ojhdtapp.parabox.domain.model.message_content

interface MessageContent {
    abstract fun getContentString() : String
}

fun List<MessageContent>.getContentString() : String{
    val builder = StringBuilder()
    forEach {
        builder.append(it.getContentString())
    }
    return builder.toString()
}