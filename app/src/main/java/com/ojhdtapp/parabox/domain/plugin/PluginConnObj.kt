package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.service.ConnKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PluginConnObj(private val ctx: Context, private val pkg: String, private val cls: String) {
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
    private var sMessenger: Messenger? = null
    private val cMessenger = Messenger(ConnHandler())
    private var isConnected = false
    private var runningStatus = AppModel.RUNNING_STATUS_DISABLED

//    fun send(str: String) {
//        if (sMessenger == null) {
//            throw RemoteException("not connected")
//        }
//        try {
//            sMessenger!!.send(Message().apply {
//                replyTo = cMessenger
//                obj = Bundle().apply {
//                    putString("str", str)
//                }
//            })
//            Log.d("parabox", "message $str sent")
//        } catch (e: RemoteException) {
//            e.printStackTrace()
//        }
//    }

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

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ConnKey.MSG_COMMAND -> {}
                ConnKey.MSG_RESPONSE -> {
                    when ((msg.obj as Bundle).getInt("command", -1)) {
                        ConnKey.MSG_RESPONSE_CHECK_RUNNING_STATUS -> {
                            val isRunning = (msg.obj as Bundle).getBoolean("value") ?: false
                            runningStatus =
                                if (isRunning) AppModel.RUNNING_STATUS_RUNNING else AppModel.RUNNING_STATUS_ERROR
                        }
                        else -> {}
                    }
                }
            }
            super.handleMessage(msg)
        }
    }
}
