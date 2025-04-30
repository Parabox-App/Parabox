package com.ojhdtapp.parabox.domain.built_in.td

import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage

class TdConnection : ParaboxConnection() {
    override suspend fun onInitialize(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun onSendMessage(message: SendMessage) {
        TODO("Not yet implemented")
    }
}