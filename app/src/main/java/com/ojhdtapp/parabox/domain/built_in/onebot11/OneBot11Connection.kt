package com.ojhdtapp.parabox.domain.built_in.onebot11

import android.net.Uri
import android.os.Bundle
import android.util.Log
import cn.chuanwise.onebot.lib.awaitUtilConnected
import cn.chuanwise.onebot.lib.v11.GROUP_MESSAGE_EVENT
import cn.chuanwise.onebot.lib.v11.LIFECYCLE_META_EVENT
import cn.chuanwise.onebot.lib.v11.META_EVENT
import cn.chuanwise.onebot.lib.v11.OneBot11AppReverseWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnectionConfiguration
import cn.chuanwise.onebot.lib.v11.PRIVATE_MESSAGE_EVENT
import cn.chuanwise.onebot.lib.v11.data.event.GroupMessageEventData
import cn.chuanwise.onebot.lib.v11.data.event.HeartbeatEventData
import cn.chuanwise.onebot.lib.v11.data.event.LifecycleMetaEventData
import cn.chuanwise.onebot.lib.v11.data.event.PrivateMessageEventData
import cn.chuanwise.onebot.lib.v11.data.message.ArrayMessageData
import cn.chuanwise.onebot.lib.v11.data.message.AtData
import cn.chuanwise.onebot.lib.v11.data.message.CqCodeMessageData
import cn.chuanwise.onebot.lib.v11.data.message.ImageData
import cn.chuanwise.onebot.lib.v11.data.message.LocationData
import cn.chuanwise.onebot.lib.v11.data.message.VideoData
import cn.chuanwise.onebot.lib.v11.data.message.RecordData
import cn.chuanwise.onebot.lib.v11.data.message.MessageData
import cn.chuanwise.onebot.lib.v11.data.message.SegmentData
import cn.chuanwise.onebot.lib.v11.data.message.SingleMessageData
import cn.chuanwise.onebot.lib.v11.data.message.TextData
import cn.chuanwise.onebot.lib.v11.getGroupInfo
import cn.chuanwise.onebot.lib.v11.getStrangerInfo
import cn.chuanwise.onebot.lib.v11.registerListener
import cn.chuanwise.onebot.lib.v11.registerListenerWithoutQuickOperation
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.optBooleanOrNull
import com.ojhdtapp.parabox.core.util.optStringOrNull
import com.ojhdtapp.parabox.domain.built_in.onebot11.util.CompatibilityUtil
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OneBot11Connection : ParaboxConnection() {
    private var appWebSocketConnection: OneBot11AppWebSocketConnection? = null
    private var appReverseWebSocketConnection: OneBot11AppReverseWebSocketConnection? = null
    lateinit var compatibilityUtil: CompatibilityUtil
    override suspend fun onInitialize(): Boolean {
        compatibilityUtil = CompatibilityUtil(enabled = extra.optBooleanOrNull("compatibility_mode") ?: false)
        val host = extra.optStringOrNull("host") ?: run {
            updateStatus(ParaboxConnectionStatus.Error("Host is not provided"))
            return false
        }
        val port = extra.optStringOrNull("port")?.toIntOrNull() ?: run {
            updateStatus(ParaboxConnectionStatus.Error("Port is not provided"))
            return false
        }
        val token = extra.optStringOrNull("token")
//                val reverseHost = extra.getString("reverse_host") ?: run {
//                    updateStatus(ParaboxExtensionStatus.Error("Reverse host is not provided"))
//                    return false
//                }
//                val reversePort = extra.getString("reverse_port")?.toIntOrNull() ?: run {
//                    updateStatus(ParaboxExtensionStatus.Error("Reverse port is not provided"))
//                    return false
//                }
        val heartInterval = extra.optStringOrNull("heart_interval")?.toIntOrNull() ?: 10
        val appWebSocketConnectionConfiguration = OneBot11AppWebSocketConnectionConfiguration(
            host = host,
            port = port,
            accessToken = token,
            heartbeatInterval = heartInterval.seconds,
        )
//                val appReverseWebSocketConnectionConfiguration = OneBot11AppReverseWebSocketConnectionConfiguration(
//                    host = reverseHost,
//                    port = reversePort,
//                )
//                appReverseWebSocketConnection = OneBot11AppReverseWebSocketConnection(appReverseWebSocketConnectionConfiguration)
        coroutineScope.launch(Dispatchers.IO) {
//            val job = SupervisorJob()
//            val coroutineScope = CoroutineScope(job + Dispatchers.IO)
            appWebSocketConnection = OneBot11AppWebSocketConnection(
                configuration = appWebSocketConnectionConfiguration,
                job = job,
                coroutineContext = coroutineScope.coroutineContext
            ).awaitUtilConnected()
            registerListener()
        }
//                appReverseWebSocketConnection!!.awaitUtilConnected()
        return true
    }

    private fun registerListener() {
        appWebSocketConnection?.incomingChannel?.registerListener(META_EVENT) {
            when (it) {
                is LifecycleMetaEventData -> {
                    Log.d("parabox", "receive lifecycle meta event:${it.subType}")
                    when (it.subType) {
                        "enable" -> {

                        }

                        "disable" -> {
                            updateStatus(ParaboxConnectionStatus.Error("OneBot disabled"))
                            if (extra.optBooleanOrNull("auto_reconnect") == true) {
//                                appWebSocketConnection?.close()
//                                appWebSocketConnection = null
//                                coroutineScope.launch(Dispatchers.IO) {
//                                    onInitialize()
//                                }
                                delay(5000)
                                Log.d("parabox", "reconnect")
                                appWebSocketConnection?.awaitUtilConnected()
                            }
                        }

                        "connect" -> {
                            updateStatus(ParaboxConnectionStatus.Active)
                        }
                    }
                }

                is HeartbeatEventData -> {
                    Log.d("parabox", "receive heartbeat, status=${it.status}")
                }
            }
        }
        appWebSocketConnection?.incomingChannel?.registerListenerWithoutQuickOperation(PRIVATE_MESSAGE_EVENT) {
            Log.d("parabox", "receive private message")
            coroutineScope.launch(Dispatchers.IO) {
                receivePrivateMessage(it)
            }
        }
        appWebSocketConnection?.incomingChannel?.registerListenerWithoutQuickOperation(GROUP_MESSAGE_EVENT) {
            Log.d("parabox", "receive group message")
            receiveGroupMessage(it)
        }
    }

    private suspend fun receivePrivateMessage(data: PrivateMessageEventData): ParaboxResult {
        val obj = ReceiveMessage(
            contents = data.message.toParaboxMessageElementList(),
            sender = ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = data.sender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = data.sender.userId.toString()
            ),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = data.sender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = ParaboxChat.TYPE_PRIVATE,
                uid = data.sender.userId.toString()
            ),
            timestamp = data.time,
            uuid = data.messageId.toString()
        )
        return receiveMessage(obj)
    }

    private suspend fun receiveGroupMessage(data: GroupMessageEventData): ParaboxResult {
        val obj = ReceiveMessage(
            contents = data.message.toParaboxMessageElementList(),
            sender = ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = data.sender.nickname,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = data.sender.userId.toString()
            ),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = null,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = ParaboxChat.TYPE_GROUP,
                uid = data.groupId.toString()
            ),
            timestamp = data.time * 1000,
            uuid = data.messageId.toString()
        )
        Log.d("parabox", "receive group message:${obj.contents} at ${obj.timestamp}")
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
        return if (appWebSocketConnection?.isConnected == true) {
            appWebSocketConnection!!.getGroupInfo(groupId.toLong(), false).let {
                ParaboxBasicInfo(
                    name = it.groupName,
                    avatar = compatibilityUtil.getGroupAvatar(groupId.toLong(), 100)?.let {
                        ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it)
                    } ?: ParaboxResourceInfo.ParaboxEmptyInfo
                )
            }
        } else {
            null
        }
    }

    override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
        return if (appWebSocketConnection?.isConnected == true) {
            appWebSocketConnection!!.getStrangerInfo(userId.toLong(), false).let {
                ParaboxBasicInfo(
                    name = it.nickname,
                    avatar = compatibilityUtil.getUserAvatar(userId.toLong(), 100)?.let {
                        ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it)
                    } ?: ParaboxResourceInfo.ParaboxEmptyInfo
                )
            }
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appWebSocketConnection?.close()
        appReverseWebSocketConnection?.close()
        appWebSocketConnection = null
        appReverseWebSocketConnection = null
    }

    private fun MessageData.toParaboxMessageElementList(): List<ParaboxMessageElement> {
        val res = mutableListOf<ParaboxMessageElement>()
        when (this) {
            is SingleMessageData -> {
                res.add(data.toParaboxMessageElement())
            }

            is ArrayMessageData -> {
                res.addAll(data.map { it.data.toParaboxMessageElement() })
            }

            is CqCodeMessageData -> {

            }
        }
        return res
    }

    private fun SegmentData.toParaboxMessageElement(): ParaboxMessageElement {
        return when (this) {
            is TextData -> ParaboxPlainText(text)
            is ImageData -> {
                val remoteResource = url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(file))
                ParaboxImage(
                    fileName = FileUtil.getFileNameFromPath(file),
                    resourceInfo = remoteResource ?: localResource
                )
            }

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

            is RecordData -> {
                val remoteResource = url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(file))
                ParaboxAudio(
                    fileName = FileUtil.getFileNameFromPath(file),
                    resourceInfo = remoteResource ?: localResource
                )
            }

            is VideoData -> {
                val remoteResource = url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(file))
                ParaboxVideo(
                    fileName = FileUtil.getFileNameFromPath(file),
                    resourceInfo = remoteResource ?: localResource
                )
            }

            else -> {
                Log.d("parabox", "unsupported segment:${this}")
                ParaboxUnsupported
            }
        }
    }
}