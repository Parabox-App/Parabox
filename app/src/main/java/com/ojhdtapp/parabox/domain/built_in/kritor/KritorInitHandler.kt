package com.ojhdtapp.parabox.domain.built_in.kritor

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult

class KritorInitHandler : ParaboxInitHandler() {
    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        return listOf(
            ParaboxInitAction.TextInputAction(
                key = "host",
                title = "主动 Grpc 服务地址",
                errMsg = "",
                description = "请输入 Grpc 服务地址",
                label = "IP 地址",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                onResult = { res: String ->
                    // check res is basic ipv4 ip address
                    if (res.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的地址")
                    }
                }
            ),
            ParaboxInitAction.TextInputAction(
                key = "port",
                title = "Grpc 服务端口",
                errMsg = "",
                description = "请输入 Grpc 服务端口",
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
            )
    }

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        return listOf()
    }
}