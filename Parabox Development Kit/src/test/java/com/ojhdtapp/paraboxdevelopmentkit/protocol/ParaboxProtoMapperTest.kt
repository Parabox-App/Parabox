package com.ojhdtapp.paraboxdevelopmentkit.protocol

import android.net.Uri
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAnnotatedText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAtAll
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomainValue
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toProto
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParaboxProtoMapperTest {

    @Test
    fun roundTripAllMessageElementsAndResources() {
        val contact = ParaboxContact(
            basicInfo = ParaboxBasicInfo(
                name = "Alice",
                avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/a.png"),
            ),
            uid = "u_1001",
        )
        val chat = ParaboxChat(
            basicInfo = ParaboxBasicInfo(
                name = "Dev Group",
                avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
            ),
            type = ParaboxChat.TYPE_GROUP,
            uid = "g_2002",
        )
        val elements = listOf(
            ParaboxPlainText("Hello PEP v2"),
            ParaboxAt(contact),
            ParaboxAtAll,
            ParaboxImage(
                width = 800,
                height = 600,
                fileName = "photo.png",
                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/photo.png"),
            ),
            ParaboxAudio(
                length = 12L,
                fileName = "voice.ogg",
                fileSize = 2048L,
                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.DriveRemoteInfo("uuid-1", "/cloud/voice.ogg", 2),
            ),
            ParaboxVideo(
                length = 30L,
                fileName = "clip.mp4",
                fileSize = 409600L,
                resourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo,
            ),
            ParaboxFile(
                name = "spec.pdf",
                extension = "pdf",
                size = 102400L,
                lastModifiedTime = 1700000000L,
                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/spec.pdf"),
            ),
            ParaboxLocation(31.23, 121.47, "Shanghai", "Bund"),
            ParaboxQuoteReply(
                sender = contact,
                timestamp = 1700000001L,
                id = "msg-0",
                messages = listOf(ParaboxPlainText("quoted")),
            ),
            ParaboxForward(
                list = listOf(
                    ParaboxForward.ForwardNode(
                        sender = contact,
                        timestamp = 1700000002L,
                        id = "node-1",
                        messages = listOf(ParaboxPlainText("forwarded body")),
                    )
                )
            ),
            ParaboxAnnotatedText(list = listOf(ParaboxPlainText("Hi "), ParaboxAt(contact))),
            ParaboxUnsupported,
        )

        val receive = ReceiveMessage(
            contents = elements,
            sender = contact,
            chat = chat,
            timestamp = 1700000010L,
            uuid = "rx-1",
        )
        val decodedReceive = receive.toProto().toDomain()
        assertEquals(receive, decodedReceive)

        val send = SendMessage(
            contents = elements,
            chat = chat,
            timestamp = 1700000011L,
            uuid = "tx-1",
        )
        val decodedSend = send.toProto().toDomain()
        assertEquals(send, decodedSend)
    }

    @Test
    fun roundTripInitActionsAndConfigItems() {
        val actions = listOf(
            ParaboxInitAction.InfoAction("info", "Welcome", "", "desc"),
            ParaboxInitAction.TextInputAction("host", "Host", "", "Server IP", "IP", ParaboxInitAction.KeyboardType.NUMBER, "127.0.0.1"),
            ParaboxInitAction.SelectAction("mode", "Mode", "", "Choose", listOf("A", "B"), 1),
            ParaboxInitAction.TextInputWithImageAction(
                key = "captcha",
                title = "Captcha",
                image = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo("https://example.com/c.png"),
                label = "Code",
            ),
            ParaboxInitAction.SwitchAction("tls", "TLS", "", "Enable TLS", true),
            ParaboxInitAction.ExternalAuthAction("oauth", "Login", "", "OAuth", "com.ext.AUTH", "com.ext", "https://auth.example.com", "Sign In"),
        )
        assertEquals(actions, actions.map { it.toProto().toDomain() })

        val configs = listOf(
            ParaboxConfigItem.Category("cat", "General", "desc"),
            ParaboxConfigItem.TextInputConfigItem("host", "Host", "desc", "IP", ParaboxInitAction.KeyboardType.TEXT, "127.0.0.1"),
            ParaboxConfigItem.SelectConfigItem("mode", "Mode", "desc", listOf("WS", "HTTP"), 0),
            ParaboxConfigItem.SwitchConfigItem("reconnect", "Auto Reconnect", "desc", true),
            ParaboxConfigItem.ActionConfigItem(
                key = "logout",
                title = "Logout",
                description = "Sign out account",
                confirmModel = ParaboxConfigItem.ActionConfigItem.ConfirmModel("Confirm", "Sign out?", "Yes", "No"),
            ),
        )
        assertEquals(configs, configs.map { it.toProto().toDomain() })
    }

    @Test
    fun roundTripStatusesResultsAndConfigValues() {
        val statuses = listOf(
            ParaboxConnectionStatus.Pending,
            ParaboxConnectionStatus.Initializing,
            ParaboxConnectionStatus.Active,
            ParaboxConnectionStatus.Reconnecting(3, "Binder unavailable"),
            ParaboxConnectionStatus.Error("Auth expired"),
        )
        assertEquals(statuses, statuses.map { it.toProto().toDomain() })

        val res = ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
        assertEquals(res, res.toProto().toDomain())

        assertEquals("abc", ParaboxProtoMapper.toConfigValueProto("abc").toDomainValue())
        assertEquals(42, ParaboxProtoMapper.toConfigValueProto(42).toDomainValue())
        assertEquals(true, ParaboxProtoMapper.toConfigValueProto(true).toDomainValue())
        assertTrue(ParaboxProtoMapper.toConfigValueProto(null).toDomainValue() == null)
    }

    @Test
    fun roundTripReceiveMessageSentByMeFlag() {
        val sender = ParaboxContact(ParaboxBasicInfo("Me", ParaboxResourceInfo.ParaboxEmptyInfo), "100")
        val chat = ParaboxChat(ParaboxBasicInfo("Friend", ParaboxResourceInfo.ParaboxEmptyInfo), ParaboxChat.TYPE_PRIVATE, "200")
        val outgoing = ReceiveMessage(
            contents = listOf(ParaboxPlainText("Sent from Telegram Desktop")),
            sender = sender,
            chat = chat,
            timestamp = 1700000123L,
            uuid = "200:999",
            sentByMe = true,
        )
        val decodedOutgoing = outgoing.toProto().toDomain()
        assertEquals(outgoing, decodedOutgoing)
        assertTrue(decodedOutgoing.sentByMe)

        val incoming = outgoing.copy(uuid = "200:1000", sentByMe = false)
        val decodedIncoming = incoming.toProto().toDomain()
        assertEquals(incoming, decodedIncoming)
        assertEquals(false, decodedIncoming.sentByMe)
    }
}
