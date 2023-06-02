package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage

abstract class ParaboxExtension {
    private var mContext: Context? = null
    private var mBridge: ParaboxBridge? = null
    fun init(context: Context, bridge: ParaboxBridge) {
        mContext = context
        mBridge = bridge
    }
    abstract fun onSendMessage(message: SendMessage)
    abstract fun onRecallMessage()
    abstract fun onGetContacts()
    abstract fun onGetChats()
    abstract fun onQueryMessageHistory(uuid: String)
    abstract fun onCreate()
    abstract fun onDestroy()
}