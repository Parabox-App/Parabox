package com.ojhdtapp.paraboxdevelopmentkit.model.message

import android.os.Parcelable
import android.util.Log

sealed interface ParaboxMessageElement : Parcelable {
    companion object {
        enum class TYPE {
            AT, AT_ALL, AUDIO, FILE, IMAGE, LOCATION, PLAIN_TEXT, QUOTE_REPLY, FORWARD, ANNOTATED_TEXT
        }
    }

    fun contentToString(): String
    fun getType(): Int
}

fun List<ParaboxMessageElement>.simplifyText(): List<ParaboxMessageElement> {
    val textTemp = mutableListOf<ParaboxText>()
    val res = mutableListOf<ParaboxMessageElement>()
    forEach {
        when (it) {
            is ParaboxText -> {
                textTemp.add(it)
            }

            is ParaboxAnnotatedText -> {
                textTemp.addAll(it.list)
            }

            else -> {
                if (textTemp.isNotEmpty()) {
                    res.add(ParaboxAnnotatedText(list = textTemp.toList()))
                    textTemp.clear()
                }
                res.add(it)
            }
        }
    }
    if (textTemp.isNotEmpty()) {
        res.add(ParaboxAnnotatedText(list = textTemp.toList()))
        textTemp.clear()
    }
    return res
}