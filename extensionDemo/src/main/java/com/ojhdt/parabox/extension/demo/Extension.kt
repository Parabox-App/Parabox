package com.ojhdt.parabox.extension.demo

import android.util.Log
import cn.evole.onebot.client.config.BotConfig
import cn.evole.onebot.client.connection.WSClient
import cn.evole.onebot.client.handler.ActionHandler
import cn.evole.onebot.client.handler.EventBus
import cn.evole.onebot.client.listener.SimpleEventListener
import cn.evole.onebot.sdk.entity.ArrayMsg
import cn.evole.onebot.sdk.event.message.GroupMessageEvent
import cn.evole.onebot.sdk.event.message.MessageEvent
import cn.evole.onebot.sdk.event.message.PrivateMessageEvent
import cn.evole.onebot.sdk.util.BotUtils
import cn.evole.onebot.sdk.util.json.GsonUtil
import com.ojhdt.parabox.extension.demo.util.toParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue

class Extension : ParaboxExtension() {

    override fun onInitialized() {
        super.onInitialized()
        Log.d("ojhdt", "on Initialize")
        val botConfig = BotConfig().apply {
            url = "ws://127.0.0.1:8080"
        }
        val blockingQueue = LinkedBlockingQueue<String>() //使用队列传输数据
        val actionHandler = ActionHandler(botConfig)
        val client = WSClient(URI.create("ws://127.0.0.1:8081"), blockingQueue, actionHandler)
        val bot = client.createBot()

        val dispatchers = EventBus(blockingQueue)
        dispatchers.addListener(object : SimpleEventListener<GroupMessageEvent>() {
            override fun onMessage(event: GroupMessageEvent?) {
                if (event != null) {
                    event.arrayMsg = BotUtils.rawToJson(event.rawMessage).map {
                        GsonUtil.fromJson<ArrayMsg>(it.toString(), ArrayMsg::class.java)
                    }
                    lifecycleScope?.launch(Dispatchers.IO) {
                        receiveGroupMessage(event)
                    }
                }
            }
        })
        dispatchers.addListener(object : SimpleEventListener<PrivateMessageEvent>() {
            override fun onMessage(event: PrivateMessageEvent?) {
                if (event != null) {
                    event.arrayMsg = BotUtils.rawToJson(event.rawMessage).map {
                        GsonUtil.fromJson<ArrayMsg>(it.toString(), ArrayMsg::class.java)
                    }
                    lifecycleScope?.launch(Dispatchers.IO) {
                        receivePrivateMessage(event)
                    }
                }
            }

        })
        lifecycleScope?.launch(Dispatchers.IO) {
            client.connect()
            dispatchers.run()
            dispatchers.stop()
        }
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

    suspend fun receiveGroupMessage(msg: GroupMessageEvent): ParaboxResult {
        val obj = ReceiveMessage(
            contents = msg.arrayMsg.mapNotNull { it.toParaboxMessageElement() },
            sender = ParaboxContact(
                name = msg.sender.nickname,
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                uid = msg.sender.userId
            ),
            chat = ParaboxChat(
                name = msg.groupId.toString(),
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                type = ParaboxChat.TYPE_GROUP,
                uid = msg.groupId.toString()
            ),
            timestamp = msg.time * 1000,
            uuid = msg.messageId.toString()
        )
        return receiveMessage(obj)
    }

    suspend fun receivePrivateMessage(msg: PrivateMessageEvent) : ParaboxResult {
        val obj = ReceiveMessage(
            contents = msg.arrayMsg.mapNotNull { it.toParaboxMessageElement() },
            sender = ParaboxContact(
                name = msg.privateSender.nickname,
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                uid = msg.privateSender.userId.toString()
            ),
            chat = ParaboxChat(
                name = msg.privateSender.nickname,
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                type = ParaboxChat.TYPE_PRIVATE,
                uid = msg.privateSender.userId.toString()
            ),
            timestamp = msg.time * 1000,
            uuid = msg.messageId.toString()
        )
        return receiveMessage(obj)
    }
}