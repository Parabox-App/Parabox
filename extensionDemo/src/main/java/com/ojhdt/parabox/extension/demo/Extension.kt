package com.ojhdt.parabox.extension.demo

import android.util.Log
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.TimeUnit

class Extension : ParaboxExtension() {
    override fun onInitialized() {
//        val server = AndServer.webServer(context!!)
//            .port(5701)
//            .timeout(10, TimeUnit.SECONDS)
//            .listener(object : Server.ServerListener {
//                override fun onStarted() {
//                    Log.d("ojhdt", "server started")
//                }
//
//                override fun onStopped() {
//                }
//
//                override fun onException(e: Exception?) {
//                }
//
//            })
//            .build()
//        lifecycleScope!!.launch {
//            if (!server.isRunning) {
//                server.startup()
//            }
//        }
        lifecycleScope!!.launch {
            while (true) {
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = System.currentTimeMillis().toString())
                        ),
                        sender = ParaboxContact(
                            name = "StageGuard",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            uid = "sender_sg"
                        ),
                        chat = ParaboxChat(
                            name = "群组会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_GROUP,
                            uid = "group_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(1000)
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = System.currentTimeMillis().toString())
                        ),
                        sender = ParaboxContact(
                            name = "404E",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            uid = "sender_404e"
                        ),
                        chat = ParaboxChat(
                            name = "群组会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_GROUP,
                            uid = "group_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(1000)
            }
        }
//        lifecycleScope!!.launch(Dispatchers.IO) {
//            while (true) {
//                receiveMessage(
//                    message = ReceiveMessage(
//                        contents = listOf(
//                            ParaboxImage(
//                                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://image.baidu.com/search/down?url=https://tvax3.sinaimg.cn//large/0072Vf1pgy1foxk3wgs1qj31kw0w01jo.jpg")
//                            )
//                        ),
//                        sender = ParaboxContact(
//                            name = "Ojhdt",
//                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://blog.ojhdt.com/avatar.png"),
//                            uid = "sender_ojhdt"
//                        ),
//                        chat = ParaboxChat(
//                            name = "私聊会话",
//                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
//                            type = ParaboxChat.TYPE_PRIVATE,
//                            uid = "private_test_1"
//                        ),
//                        timestamp = System.currentTimeMillis(),
//                        uuid = System.currentTimeMillis().toString()
//                    ),
//                )
//                delay(5000)
//                receiveMessage(
//                    message = ReceiveMessage(
//                        contents = listOf(
//                            ParaboxImage(
//                                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://image.baidu.com/search/down?url=https://tvax3.sinaimg.cn//large/0072Vf1pgy1foxkfhjrg5j31hc0u0nfw.jpg")
//                            )
//                        ),
//                        sender = ParaboxContact(
//                            name = "Ojhdt",
//                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://blog.ojhdt.com/avatar.png"),
//                            uid = "sender_ojhdt"
//                        ),
//                        chat = ParaboxChat(
//                            name = "私聊会话",
//                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
//                            type = ParaboxChat.TYPE_PRIVATE,
//                            uid = "private_test_1"
//                        ),
//                        timestamp = System.currentTimeMillis(),
//                        uuid = System.currentTimeMillis().toString()
//                    ),
//                )
//                delay(5000)
//                receiveMessage(
//                    message = ReceiveMessage(
//                        contents = listOf(
//                            ParaboxImage(
//                                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://image.baidu.com/search/down?url=https://tvax3.sinaimg.cn//large/0072Vf1pgy1foxk7rv2gpj31hc0u04e6.jpg")
//                            )
//                        ),
//                        sender = ParaboxContact(
//                            name = "Ojhdt",
//                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://blog.ojhdt.com/avatar.png"),
//                            uid = "sender_ojhdt"
//                        ),
//                        chat = ParaboxChat(
//                            name = "私聊会话",
//                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
//                            type = ParaboxChat.TYPE_PRIVATE,
//                            uid = "private_test_1"
//                        ),
//                        timestamp = System.currentTimeMillis(),
//                        uuid = System.currentTimeMillis().toString()
//                    ),
//                )
//                delay(5000)
//            }
//
//        }
    }

    override fun onSendMessage(message: SendMessage) {
        TODO("Not yet implemented")
    }

    override fun onRecallMessage() {
        TODO("Not yet implemented")
    }

    override fun onGetContacts() {
        TODO("Not yet implemented")
    }

    override fun onGetChats() {
        TODO("Not yet implemented")
    }

    override fun onQueryMessageHistory(uuid: String) {
        TODO("Not yet implemented")
    }
}