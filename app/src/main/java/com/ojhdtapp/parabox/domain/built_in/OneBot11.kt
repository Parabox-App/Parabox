package com.ojhdtapp.parabox.domain.built_in

import android.content.pm.PackageInfo
import android.os.Bundle
import cn.chuanwise.onebot.lib.awaitUtilConnected
import cn.chuanwise.onebot.lib.v11.OneBot11AppReverseWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnection
import cn.chuanwise.onebot.lib.v11.OneBot11AppWebSocketConnectionConfiguration
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

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
                TODO("Not yet implemented")
            }

        }
    override val extension: ParaboxExtension
        get() = object : ParaboxExtension() {
            private var appWebSocketConnection: OneBot11AppWebSocketConnection? = null
            private val appReverseWebSocketConnection: OneBot11AppReverseWebSocketConnection? = null
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
                val host = extra.getString("host") ?: return false
                val port = extra.getString("port")?.toIntOrNull() ?: return false
                val appWebSocketConnectionConfiguration = OneBot11AppWebSocketConnectionConfiguration(
                    host = host,
                    port = port,
                )
                appWebSocketConnection = OneBot11AppWebSocketConnection(appWebSocketConnectionConfiguration).awaitUtilConnected()
                return true
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

}