package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.messagedto.ReceiveMessageDto
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.parabox.data.local.entity.MessageVerifyStateUpdate
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.service.ConnKey
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.toolkit.ParaboxKey
import com.ojhdtapp.parabox.toolkit.ParaboxMetadata
import com.ojhdtapp.parabox.toolkit.ParaboxResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class PluginConnObj(
    val onNewMessageReceived: (dto: ReceiveMessageDto) -> Unit,
    val onMessageVerifyStateUpdate: (id: Long, value: Boolean) -> Unit,
    val onRecallMessageVerifyStateUpdate: (id: Long, value: Boolean) -> Unit,
    private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val pkg: String,
    private val cls: String,
    val handleNewMessage: HandleNewMessage,
    val updateMessage: UpdateMessage,
    val deleteMessage: DeleteMessage,
) {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("parabox", "bind status: true")
            sMessenger = Messenger(p1)
            isConnected = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d("parabox", "bind status: false")
            sMessenger = null
            isConnected = false
        }
    }
    val deferredMap = mutableMapOf<Long, CompletableDeferred<ParaboxResult>>()

    private var sMessenger: Messenger? = null
    private val cMessenger = Messenger(ConnHandler())
    private var isConnected = false
    private var runningStatus = AppModel.RUNNING_STATUS_DISABLED
    fun connect() {
        val intent = Intent().apply {
            component = ComponentName(
                pkg, cls
            )
        }
//        ctx.startService(intent)
        ctx.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun getServiceConnection(): ServiceConnection = serviceConnection

    fun send(dto: SendMessageDto) {
        sendCommand(
            command = ParaboxKey.COMMAND_SEND_MESSAGE,
            extra = Bundle().apply {
                putParcelable("dto", dto)
            },
            timeoutMillis = 6000,
            onResult = {
                dto.messageId?.let { messageId ->
                    if (it is ParaboxResult.Success) {
                        updateMessage.verifiedState(messageId, true)
                    } else {
                        updateMessage.verifiedState(messageId, false)
                    }
                }
            }
        )
//        if (isConnected) {
//            val timestamp = System.currentTimeMillis()
//            sMessenger?.send(Message.obtain(null, ConnKey.MSG_MESSAGE).apply {
//                obj = Bundle().apply {
//                    putInt("command", ConnKey.MSG_MESSAGE_SEND)
//                    putLong("timestamp", timestamp)
//                }
//                data = Bundle().apply {
//                    putParcelable("value", dto)
//                }
//                replyTo = cMessenger
//            })
//        }
    }


    fun getRunningStatus(): Int {
        return runningStatus
    }

    fun refreshRunningStatus() {
        if (isConnected) {
            val timestamp = System.currentTimeMillis()
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_MESSAGE, Bundle().apply {
                putInt("command", ConnKey.MSG_MESSAGE_CHECK_RUNNING_STATUS)
                putLong("timestamp", timestamp)
            }).apply {
                replyTo = cMessenger
            })
        }
    }

    fun recall(messageId: Long) {
        sendCommand(command = ParaboxKey.COMMAND_RECALL_MESSAGE,
        extra = Bundle().apply {
            putLong("messageId", messageId)
        },
        timeoutMillis = 3000,
        onResult = {
            if (it is ParaboxResult.Success) {
                Toast.makeText(ctx, "消息已撤回", Toast.LENGTH_SHORT).show()
                deleteMessage(messageId)
            } else {
                Toast.makeText(ctx, "消息撤回失败", Toast.LENGTH_SHORT).show()
            }
        })
//        if (isConnected) {
//            val timestamp = System.currentTimeMillis()
//            sMessenger?.send(Message.obtain(null, ConnKey.MSG_MESSAGE).apply {
//                obj = Bundle().apply {
//                    putLong("message_id", messageId)
//                    putInt("command", ConnKey.MSG_MESSAGE_RECALL)
//                    putLong("timestamp", timestamp)
//                }
//                replyTo = cMessenger
//            })
//        }
    }

    fun tryAutoLogin() {
        if (isConnected) {
            val timestamp = System.currentTimeMillis()
            sMessenger?.send(Message.obtain(null, ConnKey.MSG_MESSAGE, Bundle().apply {
                putInt("command", ConnKey.MSG_MESSAGE_TRY_AUTO_LOGIN)
                putLong("timestamp", timestamp)
            }).apply {
                replyTo = cMessenger
            })
        }
    }

    fun sendCommand(
        command: Int,
        extra: Bundle = Bundle(),
        timeoutMillis: Long = 3000,
        onResult: (ParaboxResult) -> Unit
    ) {
        coroutineScope.launch {
            val timestamp = System.currentTimeMillis()
            try {
                withTimeout(timeoutMillis) {
                    val deferred = CompletableDeferred<ParaboxResult>()
                    deferredMap[timestamp] = deferred
                    coreSendCommand(timestamp, command, extra)
                    Log.d("parabox", "command sent")
                    deferred.await().also {
                        Log.d("parabox", "successfully complete")
                        onResult(it)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_TIMEOUT
                    )
                )
            } catch (e: RemoteException) {
                deferredMap[timestamp]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_DISCONNECTED
                    )
                )
            }
        }
    }

    private fun coreSendCommand(timestamp: Long, command: Int, extra: Bundle = Bundle()) {
        if (!isConnected) {
            deferredMap[timestamp]?.complete(
                ParaboxResult.Fail(
                    command, timestamp,
                    ParaboxKey.ERROR_DISCONNECTED
                )
            )
        } else {
            val msg = Message.obtain(
                null,
                command,
                ParaboxKey.CLIENT_MAIN_APP,
                ParaboxKey.TYPE_COMMAND,
                extra.apply {
                    putParcelable(
                        "metadata", ParaboxMetadata(
                            commandOrRequest = command,
                            timestamp = timestamp,
                            sender = ParaboxKey.CLIENT_CONTROLLER
                        )
                    )
                }).apply {
                replyTo = cMessenger
            }
            sMessenger!!.send(msg)
        }
    }

    fun sendRequestResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        extra: Bundle = Bundle(),
        errorCode: Int? = null
    ) {
        if (isSuccess) {
            ParaboxResult.Success(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                obj = extra,
            )
        } else {
            ParaboxResult.Fail(
                command = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                errorCode = errorCode!!
            )
        }.also {
            deferredMap[metadata.timestamp]?.complete(it)
//            coreSendCommandResponse(isSuccess, metadata, it)
        }
    }

    private fun coreSendRequestResponse(
        isSuccess: Boolean,
        metadata: ParaboxMetadata,
        result: ParaboxResult,
        extra: Bundle = Bundle()
    ) {
        val msg = Message.obtain(
            null,
            metadata.commandOrRequest,
            ParaboxKey.CLIENT_MAIN_APP,
            ParaboxKey.TYPE_REQUEST,
            extra.apply {
                putBoolean("isSuccess", isSuccess)
                putParcelable("metadata", metadata)
                putParcelable("result", result)
            }).apply {
            replyTo = cMessenger
        }
        Log.d("parabox", "send back to service")
        sMessenger?.send(msg)

    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val obj = msg.obj as Bundle
            when (msg.arg2) {
                ParaboxKey.TYPE_REQUEST -> {
                    val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                    } else {
                        obj.getParcelable<ParaboxMetadata>("metadata")!!
                    }
                    coroutineScope.launch {
                        try {
                            val deferred =
                                CompletableDeferred<ParaboxResult>()
                            deferredMap[metadata.timestamp] = deferred

                            // 指令种类判断
                            when (msg.what) {
                                ParaboxKey.REQUEST_RECEIVE_MESSAGE -> {
                                    obj.classLoader = ReceiveMessageDto::class.java.classLoader
                                    obj.getParcelable<ReceiveMessageDto>("dto").also {
                                        if (it == null) {
                                            sendRequestResponse(
                                                isSuccess = false,
                                                metadata = metadata,
                                                errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                                            )
                                        } else {
                                            Log.d("parabox", "transfer success! value: $it")
                                            handleNewMessage(it)
                                            sendRequestResponse(
                                                isSuccess = true,
                                                metadata = metadata
                                            )
//                                            onNewMessageReceived(it)
                                        }
                                    }
                                }

                                else -> {}
                            }

                            deferred.await().also {
                                Log.d("parabox", "first deferred completed")
                                val resObj = if (it is ParaboxResult.Success) {
                                    it.obj
                                } else Bundle()
                                coreSendRequestResponse(
                                    it is ParaboxResult.Success,
                                    metadata,
                                    it,
                                    resObj
                                )
                            }
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                }

                ParaboxKey.TYPE_COMMAND -> {
                    val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                    } else {
                        obj.getParcelable<ParaboxMetadata>("metadata")!!
                    }
                    val sendTimestamp = metadata.timestamp
                    val isSuccess = obj.getBoolean("isSuccess")
                    val result = if (isSuccess) {
                        obj.getParcelable<ParaboxResult.Success>("result")
                    } else {
                        obj.getParcelable<ParaboxResult.Fail>("result")
                    }
                    result?.let {
                        Log.d("parabox", "try complete second deferred")
                        deferredMap[sendTimestamp]?.complete(it)
                    }
                }
                ParaboxKey.TYPE_NOTIFICATION -> {
                    when (msg.what) {
                        ParaboxKey.NOTIFICATION_STATE_UPDATE -> {
                            val state = obj.getInt("state", ParaboxKey.STATE_ERROR)
                            val message = obj.getString("message", "")
                            Log.d("parabox", "service state changed:${state}")
                            runningStatus = when(state){
                                ParaboxKey.STATE_ERROR -> AppModel.RUNNING_STATUS_ERROR
                                ParaboxKey.STATE_LOADING -> AppModel.RUNNING_STATUS_CHECKING
                                ParaboxKey.STATE_PAUSE -> AppModel.RUNNING_STATUS_CHECKING
                                ParaboxKey.STATE_STOP -> AppModel.RUNNING_STATUS_DISABLED
                                ParaboxKey.STATE_RUNNING -> AppModel.RUNNING_STATUS_RUNNING
                                else -> AppModel.RUNNING_STATUS_DISABLED
                            }
                        }
                    }
                }
            }



//            when (msg.what) {
//                ConnKey.MSG_MESSAGE -> {
//                    when ((msg.obj as Bundle).getInt("command", -1)) {
//                        ConnKey.MSG_MESSAGE_RECEIVE -> {
//                            msg.data.classLoader = ReceiveMessageDto::class.java.classLoader
//                            msg.data.getParcelable<ReceiveMessageDto>("value")?.let {
//                                Log.d("parabox", "transfer success! value: $it")
//                                onNewMessageReceived(it)
//                            }
//                        }
//
//                        else -> {}
//                    }
//                }
//
//                ConnKey.MSG_COMMAND -> {}
//                ConnKey.MSG_RESPONSE -> {
//                    when ((msg.obj as Bundle).getInt("command", -1)) {
//                        ConnKey.MSG_RESPONSE_CHECK_RUNNING_STATUS -> {
//                            val isRunning = (msg.obj as Bundle).getBoolean("value") ?: false
//                            runningStatus =
//                                if (isRunning) AppModel.RUNNING_STATUS_RUNNING else AppModel.RUNNING_STATUS_ERROR
//                        }
//
//                        ConnKey.MSG_RESPONSE_MESSAGE_SEND -> {
//                            val stateSuccess = (msg.obj as Bundle).getBoolean("value") ?: false
//                            val messageId = (msg.obj as Bundle).getLong("message_id")
//                            onMessageVerifyStateUpdate(messageId, stateSuccess)
//                        }
//
//                        ConnKey.MSG_RESPONSE_MESSAGE_RECALL -> {
//                            val stateSuccess = (msg.obj as Bundle).getBoolean("value") ?: false
//                            val messageId = (msg.obj as Bundle).getLong("message_id")
//                            onRecallMessageVerifyStateUpdate(messageId, stateSuccess)
//                        }
//
//                        else -> {}
//                    }
//                }
//            }
            super.handleMessage(msg)
        }
    }
}
