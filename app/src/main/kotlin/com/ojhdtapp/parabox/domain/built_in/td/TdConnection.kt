package com.ojhdtapp.parabox.domain.built_in.td

import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdConnection : ParaboxConnection(), Client.ResultHandler {
    override suspend fun onInitialize(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun onSendMessage(message: SendMessage) {
        TODO("Not yet implemented")
    }

    override fun onResult(`object`: TdApi.Object?) {
        TODO("Not yet implemented")
    }
}