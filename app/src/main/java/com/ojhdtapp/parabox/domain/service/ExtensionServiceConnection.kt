package com.ojhdtapp.parabox.domain.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ExtensionServiceConnection(val context: Context) : ServiceConnection, DefaultLifecycleObserver {
    var extensionService: ExtensionService? = null
    val intent = Intent(context, ExtensionService::class.java)
    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
        extensionService =
            (p1 as ExtensionService.ExtensionServiceBinder).getService().also {
                it.setBridge(object : ExtensionService.ExtensionServiceBridge {

                })
            }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        extensionService = null
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        context.startService(intent)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("parabox", "service connected")
        context.bindService(intent, this,
            AppCompatActivity.BIND_AUTO_CREATE
        )

    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d("parabox", "service disconnected")
        context.unbindService(this)
    }
}