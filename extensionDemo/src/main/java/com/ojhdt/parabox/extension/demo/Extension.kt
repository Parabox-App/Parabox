package com.ojhdt.parabox.extension.demo

import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Extension : ParaboxExtension() {
    override fun onInitialized() {
        lifecycleScope!!.launch(Dispatchers.IO) {
            delay(5000)
            repeat(5) { num ->
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = "测试文本")
                        ),
                        sender = ParaboxContact(
                            name = "User_${num}",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            uid = "sender_${num}"
                        ),
                        chat = ParaboxChat(
                            name = "测试会话_${num}",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            type = ParaboxChat.TYPE_PRIVATE,
                            uid = "group_${num}"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = "msg_${num}_1"
                    ),
                )
            }
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