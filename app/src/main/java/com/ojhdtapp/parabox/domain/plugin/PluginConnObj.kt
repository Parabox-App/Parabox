package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Toast
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxUtil
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PluginConnObj(
    private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val pkg: String,
    private val cls: String,
    private val connectionType: Int,
    val handleNewMessage: HandleNewMessage,
    val updateMessage: UpdateMessage,
    val deleteMessage: DeleteMessage,
    val onRunningStatusChange: (connectionType: Int, status: Int) -> Unit,
) {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d("parabox", "bind status: true")
            sMessenger = Messenger(p1)
            isConnected = true
            sendNotification(ParaboxKey.NOTIFICATION_MAIN_APP_LAUNCH)
            getState()
            coroutineScope.launch {
                refreshMessage()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d("parabox", "bind status: false")
            sMessenger = null
            isConnected = false
        }
    }
    val deferredMap = mutableMapOf<String, CompletableDeferred<ParaboxResult>>()

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
            timeoutMillis = 10000,
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

    fun getState() {
        Log.d("parabox", "state get begin!")
        sendCommand(
            command = ParaboxKey.COMMAND_GET_STATE,
            onResult = {
                Log.d("parabox", "state result back!: $it")
                if (it is ParaboxResult.Success) {
                    val state = it.obj.getInt("state")
                    val transformState = when (state) {
                        ParaboxKey.STATE_ERROR -> AppModel.RUNNING_STATUS_ERROR
                        ParaboxKey.STATE_LOADING -> AppModel.RUNNING_STATUS_CHECKING
                        ParaboxKey.STATE_PAUSE -> AppModel.RUNNING_STATUS_CHECKING
                        ParaboxKey.STATE_STOP -> AppModel.RUNNING_STATUS_DISABLED
                        ParaboxKey.STATE_RUNNING -> AppModel.RUNNING_STATUS_RUNNING
                        else -> AppModel.RUNNING_STATUS_DISABLED
                    }
                    onRunningStatusChange(connectionType, transformState)
                }
            }
        )
    }

    suspend fun refreshMessage(): ParaboxResult = suspendCoroutine<ParaboxResult> { cont ->
        Log.d("parabox", "sMessenger welcome: $sMessenger")
        if (isConnected) {
            sendCommand(
                command = ParaboxKey.COMMAND_REFRESH_MESSAGE,
                onResult = {
                    cont.resume(it)
                }
            )
        } else {
            cont.resume(
                ParaboxResult.Fail(
                    ParaboxKey.COMMAND_REFRESH_MESSAGE,
                    System.currentTimeMillis(),
                    ParaboxKey.ERROR_DISCONNECTED
                )
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

    fun sendCommand(
        command: Int,
        extra: Bundle = Bundle(),
        timeoutMillis: Long = 3000,
        onResult: (ParaboxResult) -> Unit
    ) {
        coroutineScope.launch {
            val timestamp = System.currentTimeMillis()
            val key = "${timestamp}${ParaboxUtil.getRandomNumStr(8)}"
            try {
                withTimeout(timeoutMillis) {
                    val deferred = CompletableDeferred<ParaboxResult>()
                    deferredMap[key] = deferred
                    coreSendCommand(timestamp, key, command, extra)
                    Log.d("parabox", "command sent")
                    deferred.await().also {
                        Log.d("parabox", "successfully complete")
                        onResult(it)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                deferredMap[key]?.cancel()
                onResult(
                    ParaboxResult.Fail(
                        command,
                        timestamp,
                        ParaboxKey.ERROR_TIMEOUT
                    )
                )
            } catch (e: RemoteException) {
                deferredMap[key]?.cancel()
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

    private fun coreSendCommand(
        timestamp: Long,
        key: String,
        command: Int,
        extra: Bundle = Bundle()
    ) {
        if (!isConnected) {
            deferredMap[key]?.complete(
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
                            sender = ParaboxKey.CLIENT_MAIN_APP,
                            key = key
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
                commandOrRequest = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                obj = extra,
            )
        } else {
            ParaboxResult.Fail(
                commandOrRequest = metadata.commandOrRequest,
                timestamp = metadata.timestamp,
                errorCode = errorCode!!
            )
        }.also {
            deferredMap[metadata.key]?.complete(it)
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

    fun sendNotification(notification: Int, extra: Bundle = Bundle()) {
        val timestamp = System.currentTimeMillis()
        val msg = Message.obtain(
            null,
            notification,
            0,
            ParaboxKey.TYPE_NOTIFICATION,
            extra.apply {
                putLong("timestamp", timestamp)
            }).apply {
            replyTo = cMessenger
        }
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
                            deferredMap[metadata.key] = deferred

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
                        } catch (e: NullPointerException) {
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
                                commandOrRequest = metadata.commandOrRequest,
                                timestamp = metadata.timestamp,
                                obj = obj
                            )
                        } else {
                            ParaboxResult.Fail(
                                commandOrRequest = metadata.commandOrRequest,
                                timestamp = metadata.timestamp,
                                errorCode = errorCode
                            )
                        }
                        Log.d("parabox", "try complete second deferred")
                        deferredMap[metadata.key]?.complete(result)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }

                ParaboxKey.TYPE_NOTIFICATION -> {
                    try {
                        val timestamp = obj.getLong("timestamp")
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
                                onRunningStatusChange(connectionType, runningStatus)
                            }
                        }
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }
            }
            super.handleMessage(msg)
        }
    }
}
