package com.ojhdt.parabox.extension.demo.extension_a

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult

class InitHandler : ParaboxInitHandler() {
    override suspend fun getExtensionInitActions(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        return listOf(
            ParaboxInitAction.TextInputAction(
                key = "onebot_ip",
                title = "输入 OneBot 服务地址",
                errMsg = "",
                description = "请输入 OneBot WebSocket 服务地址",
                label = "IP 地址",
                onResult = { res: String ->
                    // check res is basic ipv4 ip address
                    if (res.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的 IP 地址")
                    }
                }
            ),
            ParaboxInitAction.TextInputAction(
                key = "onebot_port",
                title = "输入 OneBot 服务端口",
                errMsg = "",
                description = "请输入 OneBot WebSocket 服务端口",
                label = "端口",
                onResult = { res: String ->
                    // check res is basic ipv4 port
                    if (res.matches(Regex("^[0-9]{1,5}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的端口")
                    }
                }
            )
        )
    }
}