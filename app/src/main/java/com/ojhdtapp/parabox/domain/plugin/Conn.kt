package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Exception

class Conn(private val ctx: Context, private val pkg: String, private val cls: String) {
    private var sMessenger: Messenger? = null
    private val cMessenger = Messenger(ConnHandler())
    private val _connectionStateFlow = MutableStateFlow<Boolean>(false)
    val connectionStateFlow = _connectionStateFlow.asStateFlow()

    private val _messageResFlow = MutableSharedFlow<String>()
    val messageResFlow = _messageResFlow.asSharedFlow()


    fun send(str: String) {
        if (sMessenger == null) {
            throw RemoteException("not connected")
        }
        try {
            sMessenger!!.send(Message().apply {
                replyTo = cMessenger
                obj = Bundle().apply {
                    putString("str", str)
                }
            })
            Log.d("parabox", "message $str sent")
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun connect(): Boolean {
        Log.d("parabox", "try connecting")
        val intent = Intent().apply {
            component = ComponentName(
                pkg, cls
            )
        }
        return try {
            ctx.startService(intent)
            ctx.bindService(
                intent,
                object : ServiceConnection {
                    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                        Log.d("parabox", "bind status: true")
                        sMessenger = Messenger(p1)
                        _connectionStateFlow.value = true
                    }

                    override fun onServiceDisconnected(p0: ComponentName?) {
                        Log.d("parabox", "bind status: false")
                        sMessenger = null
                        _connectionStateFlow.value = false
                    }

                },
                Context.BIND_AUTO_CREATE
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isInstalled(): Boolean {
        var res = false
        val pkManager = ctx.packageManager
        try {
            pkManager.getPackageInfo(pkg, 0)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return res
    }

    inner class ConnHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val receivedMsg = (msg.obj as Bundle).getString("str") ?: "message lost"
            Log.d("parabox", "message back from client: $receivedMsg")
            _messageResFlow.tryEmit(receivedMsg)
        }
    }
}
