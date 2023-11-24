package com.ojhdt.parabox.extension.demo

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import cn.evolvefield.onebot.client.connection.ConnectFactory
import cn.evolvefield.onebot.client.core.Bot
import cn.evolvefield.onebot.client.handler.EventBus
import cn.evolvefield.onebot.client.listener.SimpleEventListener
import cn.evolvefield.onebot.client.listener.impl.GroupMessageEventListener
import cn.evole.onebot.sdk.event.message.PrivateMessageEvent
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue


class Extension : ParaboxExtension() {
    private var service: ConnectFactory? = null
    private var dispatchJob: Job? = null
    private var dispatcher: EventBus? = null
    private var bot: Bot? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }
    override fun onInitialized() {
        Log.d("demo", "onInitialized")
        val blockingQueue = LinkedBlockingQueue<String>() //使用队列传输数据
        service = ConnectFactory(blockingQueue)
        bot = service?.ws?.createBot()
        dispatcher = EventBus(blockingQueue)

        val groupMessageListener = GroupMessageEventListener() //自定义监听规则\
        groupMessageListener.addHandler("aa") { groupMessage ->
            bot?.sendGroupMsg(484641769, groupMessage.message, false);
            Log.d("ojhdt", groupMessage.toString())

        } //匹配关键字监听
        dispatcher?.addListener(groupMessageListener)
        dispatcher?.addListener(object : SimpleEventListener<PrivateMessageEvent?>() {
            override fun onMessage(privateMessage: PrivateMessageEvent?) {
                Log.d("ojhdt", privateMessage.toString())
            }
        })
        lifecycleScope?.launch(Dispatchers.IO){
            try {
                while (dispatcher?.close != true) {
                    dispatcher?.runTask()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        service?.stop()
        dispatcher?.stop()
        dispatchJob?.cancel()
        super.onDestroy(owner)
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