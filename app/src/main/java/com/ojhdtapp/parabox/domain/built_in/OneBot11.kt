package com.ojhdtapp.parabox.domain.built_in

import android.content.pm.PackageInfo
import android.os.Bundle
import cn.chuanwise.onebot.lib.v11.OneBot11AppReverseWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

class OneBot11InitHandler: ParaboxInitHandler() {

//    private val objectMapper = getObjectMapper()
//    private val configurations = objectMapper.readValue<OneBot11LibTestConfiguration>(
//        getResourceURL("configurations.json")
//    )
//
//    private val logger = KotlinLogging.logger { }
//
//    private val appWebSocketConnection: OneBot11AppWebSocketConnection by lazy {
//        OneBot11AppWebSocketConnection(configurations.appWebSocketConnection).awaitUtilConnected()
//    }
//    private val appReverseWebSocketConnection: OneBot11AppReverseWebSocketConnection by lazy {
//        OneBot11AppReverseWebSocketConnection(configurations.appReverseWebSocketConnection).awaitUtilConnected()
//    }
//
//    private val appConnection = appReverseWebSocketConnection
    override suspend fun getExtensionInitActions(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        TODO("Not yet implemented")
    }
}

class OneBot11Extension : ParaboxExtension() {
    override suspend fun onInitialize(extra: Bundle): Boolean {
        TODO("Not yet implemented")
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

    override fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo? {
        TODO("Not yet implemented")
    }

    override fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
        TODO("Not yet implemented")
    }

}