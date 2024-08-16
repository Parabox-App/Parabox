package com.ojhdtapp.paraboxdevelopmentkit.extension

import android.content.Context
import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

abstract class ParaboxConnection {
    lateinit var context: Context
    lateinit var bridge: ParaboxBridge
    lateinit var coroutineScope: CoroutineScope
    lateinit var job: Job
    lateinit var extra: JSONObject
    private var _status: MutableStateFlow<ParaboxConnectionStatus> = MutableStateFlow(ParaboxConnectionStatus.Pending)
    val status get() = _status.asStateFlow()

    suspend fun init(mContext: Context, coroutineJob: Job, mbridge: ParaboxBridge, mExtra: JSONObject) {
        coroutineScope {
            coroutineScope = this
            job = coroutineJob
            context = mContext
            bridge = mbridge
            extra = mExtra
            updateStatus(ParaboxConnectionStatus.Initializing)
            onInitialize()
        }
    }

    fun updateStatus(status: ParaboxConnectionStatus) {
        _status.value = status
    }

    suspend fun receiveMessage(message: ReceiveMessage): ParaboxResult {
        return if (bridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            bridge.receiveMessage(message)
        }
    }

    suspend fun recallMessage(uuid: String): ParaboxResult {
        return if (bridge == null) {
            ParaboxResult(
                code = ParaboxResult.ERROR_UNINITIALIZED,
                message = ParaboxResult.ERROR_UNINITIALIZED_MSG
            )
        } else {
            bridge.recallMessage(uuid)
        }
    }
    abstract suspend fun onInitialize() : Boolean
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
    open fun onDestroy(){}
}

sealed interface ParaboxConnectionStatus {
    data object Pending : ParaboxConnectionStatus
    data object Initializing : ParaboxConnectionStatus
    data object Active : ParaboxConnectionStatus
    class Error(val message: String) : ParaboxConnectionStatus
}