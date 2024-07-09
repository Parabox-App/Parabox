package com.ojhdt.parabox.extension.demo

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import cn.evole.onebot.client.config.BotConfig
import cn.evole.onebot.client.connection.WSClient
import cn.evole.onebot.client.core.Bot
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
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.URI
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class Extension : ParaboxExtension() {
    private lateinit var bot: Bot
    private lateinit var client: WSClient
    private lateinit var dispatchers: EventBus

    override suspend fun onInitialize(extra: Bundle): Boolean {
        val ip = extra.getString("onebot_ip") ?: "127.0.0.1"
        val port = extra.getString("onebot_port") ?: "8080"
        val botConfig = BotConfig().apply {
            url = "ws://${ip}:${port}"
        }
        val blockingQueue = LinkedBlockingQueue<String>() //使用队列传输数据
        val actionHandler = ActionHandler(botConfig)
        client = OneBotClient(
            URI.create("ws://${ip}:${port}"), blockingQueue, actionHandler,
            { code, reason, remote ->
                updateStatus(ParaboxExtensionStatus.Error(reason ?: "Connection closed"))
            }, { ex ->
                updateStatus(ParaboxExtensionStatus.Initializing)
            }
        )
        bot = client.createBot()
        dispatchers = EventBus(blockingQueue)
        dispatchers.addListener(object : SimpleEventListener<GroupMessageEvent>() {
            override fun onMessage(event: GroupMessageEvent?) {
                if (event != null) {
                    event.arrayMsg = BotUtils.rawToJson(event.rawMessage).map {
                        GsonUtil.fromJson<ArrayMsg>(it.toString(), ArrayMsg::class.java)
                    }
                    coroutineScope?.launch(Dispatchers.IO) {
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
                    coroutineScope?.launch(Dispatchers.IO) {
                        receivePrivateMessage(event)
                    }
                }
            }
        })
        coroutineScope?.launch(Dispatchers.IO) {
            client.connect()
            updateStatus(ParaboxExtensionStatus.Active)
            dispatchers.run()
            dispatchers.stop()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
        dispatchers.stop()
    }

    override suspend fun onSendMessage(message: SendMessage) {
        TODO("Not yet implemented")
    }

    override suspend fun onRecallMessage() {
        TODO("Not yet implemented")
    }

    override suspend fun onGetContacts() {
        TODO("Not yet implemented")
    }

    override suspend fun onGetChats() {
        TODO("Not yet implemented")
    }

    override suspend fun onQueryMessageHistory(uuid: String) {
        TODO("Not yet implemented")
    }

    override suspend fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo? {
        return groupId.toLongOrNull()?.let {
            val groupInfo = bot?.getGroupInfo(it, false)
            val avatar = BotUtils.getGroupAvatar(it, 100)
            ParaboxBasicInfo(
                name = groupInfo?.data?.groupName,
                avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(avatar)
            )
        }
    }

    override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
        return userId.toLongOrNull()?.let {
            val avatar = BotUtils.getUserAvatar(it, 100)
            ParaboxBasicInfo(
                name = null,
                avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(avatar)
            )
        }
    }


    suspend fun receiveGroupMessage(msg: GroupMessageEvent): ParaboxResult {
        val obj = ReceiveMessage(
            contents = msg.arrayMsg.mapNotNull { it.toParaboxMessageElement() },
            sender = ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = msg.sender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = msg.sender.userId
            ),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = null,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = ParaboxChat.TYPE_GROUP,
                uid = msg.groupId.toString()
            ),
            timestamp = msg.time * 1000,
            uuid = msg.messageId.toString()
        )
        return receiveMessage(obj)
    }

    suspend fun receivePrivateMessage(msg: PrivateMessageEvent): ParaboxResult {
        val obj = ReceiveMessage(
            contents = msg.arrayMsg.mapNotNull { it.toParaboxMessageElement() },
            sender = ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = msg.privateSender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = msg.privateSender.userId.toString()
            ),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = msg.privateSender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = ParaboxChat.TYPE_PRIVATE,
                uid = msg.privateSender.userId.toString()
            ),
            timestamp = msg.time * 1000,
            uuid = msg.messageId.toString()
        )
        return receiveMessage(obj)
    }
}

class OneBotClient(
    uri: URI, queue: BlockingQueue<String>, actionHandler: ActionHandler,
    val mOnClose: (code: Int, reason: String?, remote: Boolean) -> Unit,
    val mOnError: (ex: Exception?) -> Unit
) : WSClient(uri, queue, actionHandler) {
    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        super.onClose(code, reason, remote)
        mOnClose(code, reason, remote)
    }

    override fun onError(ex: Exception?) {
        super.onError(ex)
        mOnError(ex)
    }
}