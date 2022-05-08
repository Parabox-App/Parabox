package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.lang.Exception
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

class Conn(private val ctx: Context, private val pkg: String, private val cls: String) {
    private var sMessenger: Messenger? = null
    private val cMessenger = Messenger(ConnHandler())
    val connectionStatusFlow = MutableStateFlow<Boolean>(false)

    var messageResFlow = MutableSharedFlow<String>()


    fun send(str: String) {
        if (sMessenger == null && !connect()) {
            throw RemoteException("connect failed")
        }
        try {
            sMessenger!!.send(Message().apply {
                replyTo = cMessenger
                obj = Bundle().apply {
                    putString("str", str)
                }
            })
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
                        sMessenger = Messenger(p1)
                        connectionStatusFlow.value = true
                    }

                    override fun onServiceDisconnected(p0: ComponentName?) {
                        sMessenger = null
                        connectionStatusFlow.value = false
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
            messageResFlow.tryEmit(receivedMsg)
        }
    }
}
