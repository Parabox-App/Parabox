package com.ojhdtapp.parabox.domain.built_in.onebot11

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult

class OneBot11InitHandler: ParaboxInitHandler() {
    override suspend fun getExtensionInitActions(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        return listOf(
            ParaboxInitAction.TextInputAction(
                key = "host",
                title = "输入 OneBot 服务地址",
                errMsg = "",
                description = "请输入 OneBot WebSocket 服务地址",
                label = "IP 地址",
                type = ParaboxInitAction.KeyboardType.NUMBER,
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
                key = "port",
                title = "输入 OneBot 服务端口",
                errMsg = "",
                description = "请输入 OneBot WebSocket 服务端口",
                label = "端口",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                onResult = { res: String ->
                    // check res is basic ipv4 port
                    if (res.matches(Regex("^[0-9]{1,5}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的端口")
                    }
                }
            ),

            ParaboxInitAction.TextInputAction(
                key = "token",
                title = "输入 AccessToken",
                errMsg = "",
                description = "请输入 Access Token（可留空）",
                label = "端口",
                onResult = { res: String ->
                    ParaboxInitActionResult.Done
                }
            )

        )
    }
}