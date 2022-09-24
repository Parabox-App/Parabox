package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Toast
import com.ojhdtapp.messagedto.ParaboxMetadata
import com.ojhdtapp.messagedto.ReceiveMessageDto
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.service.ConnKey
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.toolkit.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class PluginConnObj(
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
            // temp
            getUnreceivedMessage()
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

    fun disconnect() {
        ctx.unbindService(serviceConnection)
    }
//
//    fun getServiceConnection(): ServiceConnection = serviceConnection

    fun send(dto: SendMessageDto) {
        sendCommand(
            command = ParaboxKey.COMMAND_SEND_MESSAGE,
            extra = Bundle().apply {
                putParcelable("dto", dto)
            },
            timeoutMillis = 6000,
            onResult = {
                Log.d("parabox", "result back!:${it}")
                dto.messageId?.let { messageId ->
                    coroutineScope.launch(Dispatchers.IO) {
                        if (it is ParaboxResult.Success) {
                            updateMessage.verifiedState(messageId, true)
                        } else {
                            updateMessage.verifiedState(messageId, false)
                        }
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

    fun getUnreceivedMessage() {
        Log.d("parabox", "sMessenger welcome: $sMessenger")
        if (isConnected) {
            sendCommand(
                command = ParaboxKey.COMMAND_GET_UNRECEIVED_MESSAGE,
                onResult = {}
            )
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
                    coroutineScope.launch(Dispatchers.IO) {
                        deleteMessage(messageId)
                    }
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
                            sender = ParaboxKey.CLIENT_MAIN_APP
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
        val errorCode = if (!isSuccess) {
            (result as ParaboxResult.Fail).errorCode
        } else 0
        val msg =
            Message.obtain(
                null,
                metadata.commandOrRequest,
                ParaboxKey.CLIENT_MAIN_APP,
                ParaboxKey.TYPE_REQUEST,
                extra.apply {
                    putBoolean("isSuccess", isSuccess)
                    putParcelable("metadata", metadata)
                    putInt("errorCode", errorCode)
                }).apply {
                replyTo = cMessenger
            }
        Log.d("parabox", "send back to service")
        sMessenger?.send(msg)

    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d("parabox", "msg comming!:arg1:${msg.arg1};arg2:${msg.arg2};what:${msg.what}")
            val obj = (msg.obj as Bundle)
            when (msg.arg2) {
                ParaboxKey.TYPE_REQUEST -> {
                    Log.d("parabox", "is request")
                    coroutineScope.launch {
                        try {
                            obj.classLoader = ParaboxMetadata::class.java.classLoader
                            val metadata =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                                } else {
                                    obj.getParcelable<ParaboxMetadata>("metadata")!!
                                }
                            val deferred =
                                CompletableDeferred<ParaboxResult>()
                            deferredMap[metadata.timestamp] = deferred

                            // 指令种类判断
                            when (msg.what) {
                                ParaboxKey.REQUEST_RECEIVE_MESSAGE -> {
                                    obj.classLoader = ReceiveMessageDto::class.java.classLoader
                                    obj.getParcelable<ReceiveMessageDto>("dto").also {
                                        if (it == null) {
                                            Log.d("parabox", "message is null")
                                            sendRequestResponse(
                                                isSuccess = false,
                                                metadata = metadata,
                                                errorCode = ParaboxKey.ERROR_RESOURCE_NOT_FOUND
                                            )
                                        } else {
                                            Log.d("parabox", "transfer success! value: $it")
                                            launch(Dispatchers.IO) {
                                                handleNewMessage(it)
                                            }
                                            sendRequestResponse(
                                                isSuccess = true,
                                                metadata = metadata
                                            )
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
                        } catch (e: ClassNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }

                ParaboxKey.TYPE_COMMAND -> {
                    try {
                        obj.classLoader = ParaboxMetadata::class.java.classLoader
                        val metadata = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            obj.getParcelable("metadata", ParaboxMetadata::class.java)!!
                        } else {
                            obj.getParcelable<ParaboxMetadata>("metadata")!!
                        }
                        val sendTimestamp = metadata.timestamp
                        val isSuccess = obj.getBoolean("isSuccess")
                        val errorCode = obj.getInt("errorCode")
                        val result = if (isSuccess) {
                            ParaboxResult.Success(
                                command = metadata.commandOrRequest,
                                timestamp = metadata.timestamp,
                                obj = obj
                            )
                        } else {
                            ParaboxResult.Fail(
                                command = metadata.commandOrRequest,
                                timestamp = metadata.timestamp,
                                errorCode = errorCode
                            )
                        }
                        Log.d("parabox", "try complete second deferred")
                        deferredMap[metadata.timestamp]?.complete(result)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    }
                }

                ParaboxKey.TYPE_NOTIFICATION -> {
                    when (msg.what) {
                        ParaboxKey.NOTIFICATION_STATE_UPDATE -> {
                            val state = obj.getInt("state", ParaboxKey.STATE_ERROR)
                            val message = obj.getString("message", "")
                            Log.d("parabox", "service state changed:${state}")
                            runningStatus = when (state) {
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
            super.handleMessage(msg)
        }
    }
}
