package com.ojhdtapp.parabox.domain.built_in.kritor

import android.net.Uri
import android.util.Log
import cn.chuanwise.onebot.lib.v11.data.message.IdData
import cn.chuanwise.onebot.lib.v11.data.message.LocationData
import cn.chuanwise.onebot.lib.v11.data.message.RecordData
import cn.chuanwise.onebot.lib.v11.data.message.VideoData
import cn.chuanwise.onebot.lib.v11.deleteMessage
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.optStringOrNull
import com.ojhdtapp.parabox.domain.built_in.onebot11.util.CompatibilityUtil
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.kritor.event.Element
import io.kritor.event.ElementType
import io.kritor.event.EventServiceGrpcKt
import io.kritor.event.EventType
import io.kritor.event.MessageEvent
import io.kritor.event.Scene
import io.kritor.event.atElement
import io.kritor.event.requestPushEvent
import io.kritor.event.textElement
import io.kritor.message.MessageServiceGrpcKt
import io.kritor.message.contact
import io.kritor.message.element
import io.kritor.message.recallMessageRequest
import io.kritor.message.sendMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class KritorConnection : ParaboxConnection() {
    private val compatibilityUtil = CompatibilityUtil(true)
    private var channel: Channel? = null
    override suspend fun onInitialize(): Boolean {
        val host = extra.optStringOrNull("host") ?: run {
            updateStatus(ParaboxConnectionStatus.Error("Host is not provided"))
            return false
        }
        val port = extra.optStringOrNull("port")?.toIntOrNull() ?: run {
            updateStatus(ParaboxConnectionStatus.Error("Port is not provided"))
            return false
        }
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .enableRetry()
            .executor(Dispatchers.IO.asExecutor())
            .build()
        coroutineScope.launch(Dispatchers.IO) {
            registerListener(channel!!)
        }
        return true
    }

    private suspend fun registerListener(channel: Channel) {
        coroutineScope {
            launch {
                EventServiceGrpcKt.EventServiceCoroutineStub(channel).registerActiveListener(requestPushEvent {
                    type = EventType.EVENT_TYPE_MESSAGE
                }).collect {
                    when (it.message.scene) {
                        Scene.FRIEND, Scene.STRANGER -> {
                            receiveMessage(it.message, ParaboxChat.TYPE_PRIVATE)
                        }

                        Scene.GROUP -> {
                            receiveMessage(it.message, ParaboxChat.TYPE_GROUP)
                        }

                        else -> {}
                    }
                }
            }
        }
        coroutineScope {
            launch {
                EventServiceGrpcKt.EventServiceCoroutineStub(channel).registerActiveListener(requestPushEvent {
                    type = EventType.EVENT_TYPE_NOTICE
                }).collect {

                }
            }
        }

    }

    override suspend fun onSendMessage(message: SendMessage): Boolean {
        if (channel != null) {
            val stub = MessageServiceGrpcKt.MessageServiceCoroutineStub(channel!!)
            val res = stub.sendMessage(sendMessageRequest {

                this.contact = contact {
                    when (message.chat.type) {
                        ParaboxChat.TYPE_PRIVATE -> {
                            scene = io.kritor.message.Scene.FRIEND
                        }

                        ParaboxChat.TYPE_GROUP -> {
                            scene = io.kritor.message.Scene.GROUP
                        }
                    }
                    peer = message.chat.uid
                }
                message.contents.forEach {
                    when (it) {
                        is ParaboxPlainText -> elements.add(element {
                            type = io.kritor.message.ElementType.TEXT
                            text = io.kritor.message.textElement { text = it.text }
                        })

                        is ParaboxAt -> elements.add(element {
                            type = io.kritor.message.ElementType.AT
                            at = io.kritor.message.atElement { uid = it.target.uid }
                        })

                        else -> {}
                    }
                }

            })
            Log.d(TAG, "onSendMessage: $res")
            return true
        } else {
            return false
        }
    }

    override suspend fun onRecallMessage(uuid: String): Boolean {
        if (channel != null) {
            uuid.toLongOrNull()?.let {
                val stub = MessageServiceGrpcKt.MessageServiceCoroutineStub(channel!!)
                val res = stub.recallMessage(recallMessageRequest {
                    messageId = it
                })
                Log.d(TAG, "onRecallMessage: $res")
                return true
            } ?: return false
        } else {
            return false
        }
    }

    private suspend fun receiveMessage(data: MessageEvent, type: Int) {
        val obj = ReceiveMessage(
            contents = data.elementsList.toParaboxMessageElementList(),
            sender = ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = data.sender.nick,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = data.sender.uid.toString()
            ),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = null,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = type,
                uid = data.contact.peer.toString()
            ),
            timestamp = data.time.toLong(),
            uuid = data.messageId.toString()
        )
        Log.d(TAG, "receiveMessage: ${obj.contents} at ${obj.timestamp}")
        receiveMessage(obj)
    }

    private fun List<Element>.toParaboxMessageElementList(): List<ParaboxMessageElement> {
        return map {
            when (it.type) {
                ElementType.TEXT -> ParaboxPlainText(it.text.text)
                ElementType.IMAGE -> {
                    val remoteResource = it.image.url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                    val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(it.image.file))
                    ParaboxImage(
                        fileName = FileUtil.getFileNameFromPath(it.image.file),
                        resourceInfo = remoteResource ?: localResource
                    )
                }

                ElementType.AT -> ParaboxAt(
                    target = ParaboxContact(
                        basicInfo = ParaboxBasicInfo(
                            name = null,
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                        ),
                        uid = it.at.uid
                    )
                )

                ElementType.LOCATION -> {
                    val mLat = it.location.lat.toDouble()
                    val mLon = it.location.lon.toDouble()
                    ParaboxLocation(
                        latitude = mLat,
                        longitude = mLon,
                        name = it.location.title,
                        description = it.location.address
                    )
                }

                ElementType.VOICE -> {
                    val remoteResource = it.voice.url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                    val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(it.voice.file))
                    ParaboxAudio(
                        fileName = FileUtil.getFileNameFromPath(it.voice.file),
                        resourceInfo = remoteResource ?: localResource
                    )
                }

                ElementType.VIDEO -> {
                    val remoteResource = it.video.url?.let { ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(it) }
                    val localResource = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(it.video.file))
                    ParaboxVideo(
                        fileName = FileUtil.getFileNameFromPath(it.video.file),
                        resourceInfo = remoteResource ?: localResource
                    )
                }

                ElementType.FACE -> {
                    val faceText = compatibilityUtil.queryFace(it.face.id)
                    if (faceText != null) {
                        ParaboxPlainText(faceText)
                    } else {
                        ParaboxUnsupported
                    }
                }

                ElementType.UNRECOGNIZED -> ParaboxUnsupported
                else -> ParaboxUnsupported
            }
        }
    }

    companion object {
        const val TAG = "KritorConnection"
    }
}