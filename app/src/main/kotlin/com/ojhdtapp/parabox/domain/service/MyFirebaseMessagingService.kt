package com.ojhdtapp.parabox.domain.service

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.ojhdtapp.parabox.core.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var gson: Gson

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {

        super.onNewToken(token)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }

    //    private fun bindOnceAndSend(dto: SendMessageDto) {
//        bindService(
//            Intent(this, ExtensionService::class.kotlin),
//            object : ServiceConnection {
//                override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
//                    val service = (p1 as ExtensionService.PluginServiceBinder).getService()
//                    service.sendMessage(dto)
//                    unbindService(this)
//                }
//
//                override fun onServiceDisconnected(p0: ComponentName?) {
//                }
//            },
//            BIND_AUTO_CREATE
//        )
//    }
}