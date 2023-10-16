package com.ojhdtapp.paraboxdevelopmentkit.model.message

import android.os.Parcelable

sealed interface ParaboxMessageElement: Parcelable{
    companion object{
        enum class TYPE {
            AT, AT_ALL, AUDIO, FILE, IMAGE, LOCATION, PLAIN_TEXT, QUOTE_REPLY, FORWARD
        }
    }
    fun contentToString(): String
    fun getType(): Int
}