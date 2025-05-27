package com.ojhdtapp.paraboxdevelopmentkit.extension

import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ReceivePureMessage

interface ParaboxBridge {
    suspend fun receiveMessage(message: ReceiveMessage) : ParaboxResult
    suspend fun receivePureMessage(msg: ReceivePureMessage): ParaboxResult
    suspend fun receiveContact(contact: ParaboxContact): ParaboxResult
    suspend fun receiveChat(chat: ParaboxChat): ParaboxResult
    suspend fun updateChatLatestMessage(chatUid: String, messageUid: String) : ParaboxResult

    suspend fun recallMessage(uuid: String) : ParaboxResult
}