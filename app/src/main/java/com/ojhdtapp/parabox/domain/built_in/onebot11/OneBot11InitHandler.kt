package com.ojhdtapp.parabox.domain.built_in.onebot11

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult

class OneBot11InitHandler: ParaboxInitHandler() {
    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        data.putBoolean("compatibility_mode", false)
        return listOf(
            ParaboxInitAction.TextInputAction(
                key = "host",
                title = "输入正向 WebSocket 服务地址",
                errMsg = "",
                description = "请输入 OneBot 正向 WebSocket 服务地址",
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
                description = "请输入 OneBot 正向 WebSocket 服务端口",
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

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        return listOf(
            ParaboxConfigItem.Category(
                key = "category_general",
                title = "通用",
                description = "通用配置"
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = "host",
                title = "OneBot 服务地址",
                description = "OneBot 正向 WebSocket 服务地址",
                label = "IP 地址",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                defaultValue = null,
                onResult = { res: String ->
                    // check res is basic ipv4 ip address
                    if (res.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的 IP 地址")
                    }
                }
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = "port",
                title = "OneBot 服务端口",
                description = "OneBot 正向 WebSocket 服务端口",
                label = "端口",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                defaultValue = null,
                onResult = { res: String ->
                    // check res is basic ipv4 port
                    if (res.matches(Regex("^[0-9]{1,5}$"))) {
                        ParaboxInitActionResult.Done
                    } else {
                        ParaboxInitActionResult.Error("请输入正确的端口")
                    }
                }
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = "token",
                title = "AccessToken",
                description = "Access Token（可留空）",
                label = "Token",
                defaultValue = null,
                onResult = { res: String ->
                    ParaboxInitActionResult.Done
                }
            ),
            ParaboxConfigItem.Category(
                key = "category_advanced",
                title = "高级",
                description = "高级配置"
            ),
            ParaboxConfigItem.SwitchConfigItem(
                key = "compatibility_mode",
                title = "兼容模式",
                description = "启用协议之外的兼容逻辑",
                defaultValue = false
            )
        )
    }
}