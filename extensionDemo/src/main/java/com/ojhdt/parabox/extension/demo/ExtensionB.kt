package com.ojhdt.parabox.extension.demo

import android.util.Log
import cn.evole.onebot.client.config.BotConfig
import cn.evole.onebot.client.connection.WSClient
import cn.evole.onebot.client.handler.ActionHandler
import cn.evole.onebot.client.handler.EventBus
import cn.evole.onebot.client.listener.SimpleEventListener
import cn.evole.onebot.sdk.event.message.GroupMessageEvent
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue

class ExtensionB : ParaboxExtension() {

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
        dispatchers.addListener(object: SimpleEventListener<GroupMessageEvent>(){
            override fun onMessage(p0: GroupMessageEvent?) {
                Log.d("ojhdt", "event:$p0;raw:${p0?.rawMessage};arrayMsg:${p0?.arrayMsg}")
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
}