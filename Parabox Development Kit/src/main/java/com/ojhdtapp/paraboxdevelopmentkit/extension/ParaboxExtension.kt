package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.BuildConfig
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class ParaboxExtension {
    private var mContext: Context? = null
    private var mBridge: ParaboxBridge? = null
    var coroutineScope: CoroutineScope? = null
        private set
    private var _status: MutableStateFlow<ParaboxExtensionStatus> = MutableStateFlow(ParaboxExtensionStatus.Pending)
    val status get() = _status.asStateFlow()

    suspend fun init(context: Context, bridge: ParaboxBridge, extra: Bundle) {
        coroutineScope {
            if (BuildConfig.DEBUG) {
                launch {
                    while (true) {
                        delay(5000)
                        Log.d("parabox",
                            " ${coroutineContext[CoroutineName.Key]} is executing on thread : ${Thread.currentThread().id}"
                        )
                    }
                }
            }
            coroutineScope = this
            mContext = context
            mBridge = bridge
            updateStatus(ParaboxExtensionStatus.Initializing)
            onInitialize(extra)
        }
    }

    fun updateStatus(status: ParaboxExtensionStatus) {
        _status.value = status
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
    abstract suspend fun onInitialize(extra: Bundle) : Boolean
    abstract suspend fun onSendMessage(message: SendMessage)
    abstract suspend fun onRecallMessage()
    abstract suspend fun onGetContacts()
    abstract suspend fun onGetChats()
    abstract suspend fun onQueryMessageHistory(uuid: String)
    abstract suspend fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo?
    abstract suspend fun onGetUserBasicInfo(userId: String) : ParaboxBasicInfo?

    open fun onCreate(){}
    open fun onStart(){}
    open fun onResume(){}
    open fun onPause(){}
    open fun onStop(){}
    open fun onDestroy() {
        mContext = null
        mBridge = null
        coroutineScope = null
    }
}

sealed interface ParaboxExtensionStatus {
    data object Pending : ParaboxExtensionStatus
    data object Initializing : ParaboxExtensionStatus
    data object Active : ParaboxExtensionStatus
    class Error(val message: String) : ParaboxExtensionStatus
}