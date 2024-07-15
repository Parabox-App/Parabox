package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ojhdtapp.paraboxdevelopmentkit.BuildConfig
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class ParaboxConnection {
    private var mContext: Context? = null
    private var mBridge: ParaboxBridge? = null
    var coroutineScope: CoroutineScope? = null
        private set
    private var _status: MutableStateFlow<ParaboxConnectionStatus> = MutableStateFlow(ParaboxConnectionStatus.Pending)
    val status get() = _status.asStateFlow()

    suspend fun init(context: Context, bridge: ParaboxBridge, extra: Bundle) {
        coroutineScope {
            coroutineScope = this
            mContext = context
            mBridge = bridge
            updateStatus(ParaboxConnectionStatus.Initializing)
            onInitialize(extra)
        }
    }

    fun updateStatus(status: ParaboxConnectionStatus) {
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

sealed interface ParaboxConnectionStatus {
    data object Pending : ParaboxConnectionStatus
    data object Initializing : ParaboxConnectionStatus
    data object Active : ParaboxConnectionStatus
    class Error(val message: String) : ParaboxConnectionStatus
}