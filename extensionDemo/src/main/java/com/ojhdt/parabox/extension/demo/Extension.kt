package com.ojhdt.parabox.extension.demo

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import cn.evole.onebot.client.config.BotConfig
import cn.evole.onebot.client.connection.ConnectFactory
import cn.evole.onebot.client.core.Bot
import cn.evole.onebot.client.handler.EventBus
import cn.evole.onebot.client.handler.Handler
import cn.evole.onebot.client.listener.SimpleEventListener
import cn.evole.onebot.client.listener.impl.GroupMessageEventListener
import cn.evole.onebot.sdk.entity.ArrayMsg
import cn.evole.onebot.sdk.entity.MsgChainBean
import cn.evole.onebot.sdk.enums.MsgTypeEnum
import cn.evole.onebot.sdk.event.message.GroupMessageEvent
import cn.evole.onebot.sdk.util.BotUtils
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue


class Extension : ParaboxExtension() {
    private var bot: Bot? = null
    override fun onInitialized() {
        val blockingQueue = LinkedBlockingQueue<String>() //使用队列传输数据

        val service = ConnectFactory(
            BotConfig("ws://127.0.0.1:5800"), blockingQueue
        ) //创建websocket客户端
        bot = service.ws.createBot()
        val dispatchers = EventBus(blockingQueue) //创建事件分发器
        dispatchers.addListener(object : SimpleEventListener<GroupMessageEvent>() {
            override fun onMessage(t: GroupMessageEvent?) {
                if (t != null) {
                    t.arrayMsg = BotUtils.stringToMsgChain(t.rawMessage)
                    lifecycleScope?.launch(Dispatchers.IO) {
                        receiveGroupMessage(t)
                    }
                }
            }
        })
        lifecycleScope?.launch(Dispatchers.IO) {
            dispatchers.run()
            dispatchers.stop()
            service.stop()
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
            contents = msg.arrayMsg.map { it.toParaboxMessageElement() }.filterNotNull(),
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
            timestamp = msg.time,
            uuid = msg.messageId.toString()
        )
        return receiveMessage(obj)
    }
}

fun MsgChainBean.toParaboxMessageElement(): ParaboxMessageElement? {
    return try {
        when (type) {
            MsgTypeEnum.at.name -> ParaboxAt(
                target = ParaboxContact(
                    name = data.get("qq")!!,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                    uid = data.get("qq")!!
                )
            )

            MsgTypeEnum.text.name -> ParaboxPlainText(text = data.get("text")!!)
            MsgTypeEnum.image.name -> ParaboxImage(
                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
                    url = data.get("url")!!
                )
            )

            MsgTypeEnum.reply.name -> ParaboxQuoteReply(
                belong = ParaboxContact(
                    name = "Unknown",
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                    uid = ""
                ), messageUUID = data.get("id")!!
            )

            else -> ParaboxPlainText("不支持的类型")
        }
    } catch (e: Exception) {
        null
    }

}