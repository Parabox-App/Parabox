import android.util.Log
import cn.evole.onebot.client.config.BotConfig
import cn.evole.onebot.client.connection.WSClient
import cn.evole.onebot.client.core.Bot
import cn.evole.onebot.client.handler.ActionHandler
import cn.evole.onebot.client.handler.EventBus
import cn.evole.onebot.client.listener.SimpleEventListener
import cn.evole.onebot.sdk.entity.ArrayMsg
import cn.evole.onebot.sdk.event.message.GroupMessageEvent
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue


class Extension : ParaboxExtension() {

    private var bot: Bot? = null
    override fun onInitialized() {

        val botConfig = BotConfig()
        val blockingQueue = LinkedBlockingQueue<String>() //使用队列传输数据
        val actionHandler = ActionHandler(botConfig)
        val client = WSClient(URI.create("ws://127.0.0.1:8081"), blockingQueue, actionHandler)
        bot = client.createBot()

        val dispatchers = EventBus(blockingQueue)
        dispatchers.addListener(object: SimpleEventListener<GroupMessageEvent>(){
            override fun onMessage(p0: GroupMessageEvent?) {
                Log.d("ojhdt", "event:$p0;raw:${p0?.rawMessage};arrayMsg:${p0?.arrayMsg}")
            }
        })
//
//        val service = ConnectFactory(
//            BotConfig("ws://127.0.0.1:5800"), blockingQueue
//        ) //创建websocket客户端
//        bot = service.ws.createBot()
//        val dispatchers = EventBus(blockingQueue) //创建事件分发器
//        dispatchers.addListener(object : SimpleEventListener<GroupMessageEvent>() {
//            override fun onMessage(t: GroupMessageEvent?) {
//                if (t != null) {
//                    t.arrayMsg = GsonUtil.getGson().fromJson<List<ArrayMsg>>(t.message, object : TypeToken<List<ArrayMsg?>?>() {}.type)
//                        .map { MsgChainBean().apply {
//                        type = it.type.name
//                        data = it.data
//                    } }
//                    lifecycleScope?.launch(Dispatchers.IO) {
//                        receiveGroupMessage(t)
//                    }
//                }
//            }
//        })
//        lifecycleScope?.launch(Dispatchers.IO) {
//            dispatchers.run()
//            dispatchers.stop()
//            service.stop()
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

fun ArrayMsg.toParaboxMessageElement(): ParaboxMessageElement? {
    return try {
        when (type) {
//            MsgTypeEnum.at.name -> ParaboxAt(
//                target = ParaboxContact(
//                    name = data.get("qq")!!,
//                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
//                    uid = data.get("qq")!!
//                )
//            )
//
//            MsgTypeEnum.text.name -> ParaboxPlainText(text = data.get("text")!!)
//            MsgTypeEnum.image.name -> ParaboxImage(
//                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
//                    url = data.get("url")!!
//                )
//            )
//
//            MsgTypeEnum.reply.name -> ParaboxQuoteReply(
//                belong = ParaboxContact(
//                    name = "Unknown",
//                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
//                    uid = ""
//                ), messageUUID = data.get("id")!!
//            )

            else -> ParaboxPlainText("不支持的类型")
        }
    } catch (e: Exception) {
        null
    }

}