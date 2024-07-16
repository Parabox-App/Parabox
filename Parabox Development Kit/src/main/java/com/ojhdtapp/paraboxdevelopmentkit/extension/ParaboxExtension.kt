package com.ojhdtapp.paraboxdevelopmentkit.extension

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler

interface ParaboxExtension {
    fun getKey(): String
    fun getName(): String?
    fun getDescription(): String?
    fun getIconResId(): Int? {
        return null
    }
    fun getInitHandler(): ParaboxInitHandler
    fun getConnectionClassName(): String
}