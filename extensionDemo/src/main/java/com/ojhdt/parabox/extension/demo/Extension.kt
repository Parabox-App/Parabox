package com.ojhdt.parabox.extension.demo

import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Extension : ParaboxExtension() {
    override fun onInitialized() {
        lifecycleScope!!.launch(Dispatchers.IO) {
            while(true){
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = "测试文本")
                        ),
                        sender = ParaboxContact(
                            name = "Ojhdt",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            uid = "sender_ojhdt"
                        ),
                        chat = ParaboxChat(
                            name = "私聊会话",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ui-avatars.com/api/?name=P"),
                            type = ParaboxChat.TYPE_PRIVATE,
                            uid = "private_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(3000)
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxImage(
                                resourceInfo = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://tuapi.eees.cc/api.php?category=dongman&type=302")
                            )
                        ),
                        sender = ParaboxContact(
                            name = "Ojhdt",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            uid = "sender_ojhdt"
                        ),
                        chat = ParaboxChat(
                            name = "私聊会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_PRIVATE,
                            uid = "private_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(10000)
            }

        }
        lifecycleScope!!.launch(Dispatchers.IO){
            while(true){
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = System.currentTimeMillis().toString())
                        ),
                        sender = ParaboxContact(
                            name = "StageGuard",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            uid = "sender_sg"
                        ),
                        chat = ParaboxChat(
                            name = "群组会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_GROUP,
                            uid = "group_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(1000)
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = System.currentTimeMillis().toString())
                        ),
                        sender = ParaboxContact(
                            name = "404E",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            uid = "sender_404e"
                        ),
                        chat = ParaboxChat(
                            name = "群组会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_GROUP,
                            uid = "group_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(1000)
                receiveMessage(
                    message = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = System.currentTimeMillis().toString())
                        ),
                        sender = ParaboxContact(
                            name = "LaoLittle",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            uid = "sender_llt"
                        ),
                        chat = ParaboxChat(
                            name = "群组会话",
                            avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                            type = ParaboxChat.TYPE_GROUP,
                            uid = "group_test_1"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = System.currentTimeMillis().toString()
                    ),
                )
                delay(1000)
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