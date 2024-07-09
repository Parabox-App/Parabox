package com.ojhdtapp.parabox.domain.built_in

import android.content.pm.PackageInfo
import android.os.Bundle
import android.util.Log
import cn.chuanwise.onebot.lib.awaitUtilConnected
import cn.chuanwise.onebot.lib.v11.GROUP_MESSAGE_EVENT
import cn.chuanwise.onebot.lib.v11.HEARTBEAT_META_EVENT
import cn.chuanwise.onebot.lib.v11.META_EVENT
import cn.chuanwise.onebot.lib.v11.OneBot11AppReverseWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppReverseWebSocketConnectionConfiguration
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnectionConfiguration
import cn.chuanwise.onebot.lib.v11.PRIVATE_MESSAGE_EVENT
import cn.chuanwise.onebot.lib.v11.data.event.GroupMessageEventData
import cn.chuanwise.onebot.lib.v11.data.event.PrivateMessageEventData
import cn.chuanwise.onebot.lib.v11.data.message.ArrayMessageData
import cn.chuanwise.onebot.lib.v11.data.message.AtData
import cn.chuanwise.onebot.lib.v11.data.message.CQCodeMessageData
import cn.chuanwise.onebot.lib.v11.data.message.ImageData
import cn.chuanwise.onebot.lib.v11.data.message.LocationData
import cn.chuanwise.onebot.lib.v11.data.message.MessageData
import cn.chuanwise.onebot.lib.v11.data.message.SegmentData
import cn.chuanwise.onebot.lib.v11.data.message.SingleMessageData
import cn.chuanwise.onebot.lib.v11.data.message.TextData
import cn.chuanwise.onebot.lib.v11.getGroupInfo
import cn.chuanwise.onebot.lib.v11.registerListener
import cn.chuanwise.onebot.lib.v11.registerListenerWithQuickOperation
import cn.chuanwise.onebot.lib.v11.registerListenerWithoutQuickOperation
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object OneBot11 : BuiltInConnection {
    override val key: String
        get() = "onebot11"
    override val connection: Connection.BuiltInConnection
        get() = Connection.BuiltInConnection(
            "OneBot 11",
            null,
            "通用聊天机器人应用接口标准（版本11）",
            key
        )
    override val initHandler: ParaboxInitHandler
        get() = object : ParaboxInitHandler() {
            override suspend fun getExtensionInitActions(
                list: List<ParaboxInitAction>,
                currentActionIndex: Int
            ): List<ParaboxInitAction> {
                return listOf(
                    ParaboxInitAction.TextInputAction(
                        key = "host",
                        title = "输入 OneBot 服务地址",
                        errMsg = "",
                        description = "请输入 OneBot WebSocket 服务地址",
                        label = "IP 地址",
                        onResult = { res: String ->
                            // check res is basic ipv4 ip address
                            if (res.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
                                ParaboxInitActionResult.Done
                            } else {
                                ParaboxInitActionResult.Error("请输入正确的 IP 地址")
                            }
                        }
                    ),
                    ParaboxInitAction.TextInputAction(
                        key = "port",
                        title = "输入 OneBot 服务端口",
                        errMsg = "",
                        description = "请输入 OneBot WebSocket 服务端口",
                        label = "端口",
                        onResult = { res: String ->
                            // check res is basic ipv4 port
                            if (res.matches(Regex("^[0-9]{1,5}$"))) {
                                ParaboxInitActionResult.Done
                            } else {
                                ParaboxInitActionResult.Error("请输入正确的端口")
                            }
                        }
                    )
                )
            }

        }
    override val extension: ParaboxExtension
        get() = object : ParaboxExtension() {
            private var appWebSocketConnection: OneBot11AppWebSocketConnection? = null
            private var appReverseWebSocketConnection: OneBot11AppReverseWebSocketConnection? = null
            //
//     by lazy {
//        OneBot11AppWebSocketConnection().awaitUtilConnected()
//    }
//    private val appReverseWebSocketConnection: OneBot11AppReverseWebSocketConnection by lazy {
//        OneBot11AppReverseWebSocketConnection(configurations.appReverseWebSocketConnection).awaitUtilConnected()
//    }
//
//    private val appConnection = appReverseWebSocketConnection
            override suspend fun onInitialize(extra: Bundle): Boolean {
                val host = extra.getString("host") ?: run {
                    updateStatus(ParaboxExtensionStatus.Error("Host is not provided"))
                    return false
                }
                val port = extra.getString("port")?.toIntOrNull() ?: run {
                    updateStatus(ParaboxExtensionStatus.Error("Port is not provided"))
                    return false
                }
//                val reverseHost = extra.getString("reverse_host") ?: run {
//                    updateStatus(ParaboxExtensionStatus.Error("Reverse host is not provided"))
//                    return false
//                }
//                val reversePort = extra.getString("reverse_port")?.toIntOrNull() ?: run {
//                    updateStatus(ParaboxExtensionStatus.Error("Reverse port is not provided"))
//                    return false
//                }
                val appWebSocketConnectionConfiguration = OneBot11AppWebSocketConnectionConfiguration(
                    host = host,
                    port = port,
                    maxConnectAttempts = 2
                )
//                val appReverseWebSocketConnectionConfiguration = OneBot11AppReverseWebSocketConnectionConfiguration(
//                    host = reverseHost,
//                    port = reversePort,
//                )
                appWebSocketConnection = OneBot11AppWebSocketConnection(appWebSocketConnectionConfiguration)
//                appReverseWebSocketConnection = OneBot11AppReverseWebSocketConnection(appReverseWebSocketConnectionConfiguration)
                coroutineScope?.launch(Dispatchers.IO) {
                    Log.d("parabox", "appWebSocketConnection connected")
                    registerListener()
                }
//                appReverseWebSocketConnection!!.awaitUtilConnected()
                updateStatus(ParaboxExtensionStatus.Active)
                return true
            }

            private fun registerListener() {
                appWebSocketConnection?.incomingChannel?.registerListener(META_EVENT) {
                    Log.d("parabox", "receive meta event:${it.metaEventType}")
                }
                appWebSocketConnection?.incomingChannel?.registerListenerWithoutQuickOperation(PRIVATE_MESSAGE_EVENT) {
                    Log.d("parabox", "receive private message")
                    coroutineScope?.launch(Dispatchers.IO) {
                        receivePrivateMessage(it)
                    }
                }
                appWebSocketConnection?.incomingChannel?.registerListenerWithoutQuickOperation(GROUP_MESSAGE_EVENT) {
                    Log.d("parabox", "receive group message")
                    coroutineScope?.launch(Dispatchers.IO) {
                        receiveGroupMessage(it)
                    }
                }
            }

            private suspend fun receivePrivateMessage(data: PrivateMessageEventData) : ParaboxResult {
                val obj = ReceiveMessage(
                    contents = data.message.toParaboxMessageElementList(),
                    sender = ParaboxContact(
                        basicInfo = ParaboxBasicInfo(
                            name = data.sender.nickname,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                        ),
                        uid = data.sender.userID.toString()
                    ),
                    chat = ParaboxChat(
                        basicInfo = ParaboxBasicInfo(
                            name = data.sender.nickname,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                        ),
                        type = ParaboxChat.TYPE_PRIVATE,
                        uid = data.sender.userID.toString()
                    ),
                    timestamp = data.time,
                    uuid = data.messageID.toString()
                )
                return receiveMessage(obj)
            }

            private suspend fun receiveGroupMessage(data: GroupMessageEventData) : ParaboxResult {
                val obj = ReceiveMessage(
                    contents = data.message.toParaboxMessageElementList(),
                    sender = ParaboxContact(
                        basicInfo = ParaboxBasicInfo(
                            name = data.sender.nickname,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                        ),
                        uid = data.sender.userID.toString()
                    ),
                    chat = ParaboxChat(
                        basicInfo = ParaboxBasicInfo(
                            name = null,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                        ),
                        type = ParaboxChat.TYPE_GROUP,
                        uid = data.groupID.toString()
                    ),
                    timestamp = data.time,
                    uuid = data.messageID.toString()
                )
                return receiveMessage(obj)
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
                if(appWebSocketConnection?.isConnected == true) {
                    appWebSocketConnection!!.getGroupInfo(groupId.toLong(), false).let {
                        return ParaboxBasicInfo(
                            name = it.groupName,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo
                        )
                    }
                } else {
                    return null
                }
            }

            override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
                TODO("Not yet implemented")
            }

            override fun onDestroy() {
                super.onDestroy()
                appWebSocketConnection?.close()
                appReverseWebSocketConnection?.close()
                appWebSocketConnection = null
                appReverseWebSocketConnection = null
            }

            private fun MessageData.toParaboxMessageElementList() : List<ParaboxMessageElement> {
                val res = mutableListOf<ParaboxMessageElement>()
                when(this) {
                    is SingleMessageData -> {
                        res.add(data.toParaboxMessageElement())
                    }
                    is ArrayMessageData -> {
                        res.addAll(data.map { it.data.toParaboxMessageElement() })
                    }
                    is CQCodeMessageData -> {

                    }
                }
                return res
            }

            private fun SegmentData.toParaboxMessageElement() : ParaboxMessageElement {
                return when(this) {
                    is TextData -> ParaboxPlainText(text)
                    is ImageData -> ParaboxImage(
                        resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(file)
                    )
                    is AtData -> ParaboxAt(
                        target = ParaboxContact(
                            basicInfo = ParaboxBasicInfo(
                                name = null,
                                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            ),
                            uid = qq
                        )
                    )
                    is LocationData -> {
                        val mLat = lat.toDoubleOrNull()
                        val mLon = lon.toDoubleOrNull()
                        if (mLat != null && mLon != null) {
                            ParaboxLocation(
                                latitude = mLat,
                                longitude = mLon,
                                name = title,
                                description = content
                            )
                        } else {
                            ParaboxUnsupported
                        }
                    }
                    else -> ParaboxUnsupported
                }
            }
        }
}