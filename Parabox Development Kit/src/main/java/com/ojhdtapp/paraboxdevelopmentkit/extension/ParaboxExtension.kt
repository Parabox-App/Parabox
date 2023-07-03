package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage

abstract class ParaboxExtension: DefaultLifecycleObserver {
    private var mContext: Context? = null
    private var mBridge: ParaboxBridge? = null
    fun init(context: Context, bridge: ParaboxBridge) {
        mContext = context
        mBridge = bridge
    }

    fun receiveMessage(message: ReceiveMessage): ParaboxResult {
        return if (mBridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            mBridge!!.receiveMessage(message)
        }
    }

    fun recallMessage(uuid: String): ParaboxResult {
        return if (mBridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            mBridge!!.recallMessage(uuid)
        }
    }

    abstract fun onSendMessage(message: SendMessage)
    abstract fun onRecallMessage()
    abstract fun onGetContacts()
    abstract fun onGetChats()
    abstract fun onQueryMessageHistory(uuid: String)
}