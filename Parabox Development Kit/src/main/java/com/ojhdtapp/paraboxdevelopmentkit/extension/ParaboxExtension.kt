package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import kotlinx.coroutines.flow.MutableStateFlow

abstract class ParaboxExtension: DefaultLifecycleObserver {
    private var mContext: Context? = null
    private var mBridge: ParaboxBridge? = null
    private var lifecycleOwner: LifecycleOwner? = null
    val lifecycleScope get() = lifecycleOwner?.lifecycleScope
    var isLoaded: MutableStateFlow<Boolean> = MutableStateFlow(false)
        private set
    fun init(context: Context, bridge: ParaboxBridge) {
        mContext = context
        mBridge = bridge
        isLoaded.value = true
        onInitialized()
    }

    suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
        return if (mBridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            mBridge!!.receiveMessage(message)
        }
    }

    suspend fun recallMessage(uuid: String): ParaboxResult {
        return if (mBridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            mBridge!!.recallMessage(uuid)
        }
    }
    open fun onInitialized(){}
    abstract fun onSendMessage(message: SendMessage)
    abstract fun onRecallMessage()
    abstract fun onGetContacts()
    abstract fun onGetChats()
    abstract fun onQueryMessageHistory(uuid: String)
    abstract fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo?
    abstract fun onGetUserBasicInfo(userId: String) : ParaboxBasicInfo?
    override fun onCreate(owner: LifecycleOwner) {
        lifecycleOwner = owner
        super.onCreate(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        lifecycleOwner = null
        super.onDestroy(owner)
    }
}