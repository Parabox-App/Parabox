package com.ojhdtapp.paraboxdevelopmentkit.model.message

import android.os.Parcelable
import android.util.Log

sealed interface ParaboxMessageElement : Parcelable {
    companion object {
        enum class TYPE {
            AT, AT_ALL, AUDIO, FILE, IMAGE, LOCATION, PLAIN_TEXT, QUOTE_REPLY, FORWARD, ANNOTATED_TEXT, UNSUPPORTED
        }
    }

    fun contentToString(): String
    fun getType(): Int
}

fun List<ParaboxMessageElement>.simplifyText(): List<ParaboxMessageElement?> {
    val textTemp = mutableListOf<ParaboxText>()
    val res = mutableListOf<ParaboxMessageElement?>()
    forEach {
        when (it) {
            is ParaboxText -> {
                textTemp.add(it)
            }

            is ParaboxAnnotatedText -> {
                textTemp.addAll(it.list)
            }

            else -> {
                textTemp.toList().takeIf { it.isNotEmpty() }?.let {
                    res.add(ParaboxAnnotatedText(list = it.toList()))
                    res.addAll(List(it.size - 1){null})
                    textTemp.clear()
                }
                res.add(it)
            }
        }
    }
    textTemp.toList().takeIf { it.isNotEmpty() }?.let {
        res.add(ParaboxAnnotatedText(list = it.toList()))
        res.addAll(List(it.size - 1){null})
        textTemp.clear()
    }
    return res
}