package com.ojhdtapp.parabox.domain.built_in.onebot11

import com.ojhdtapp.parabox.protocol.v2.ExtensionCapability
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionDescriptor
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxInitStepResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import org.json.JSONObject

class OneBot11ExtensionProvider : ParaboxExtensionProvider {

    override val descriptor = ParaboxExtensionDescriptor(
        key = OneBot11.KEY,
        name = "OneBot 11",
        description = "通用聊天机器人应用接口标准（版本11）",
        capabilities = setOf(ExtensionCapability.CAPABILITY_FORWARD_NODE),
    )

    private val initSteps: List<ParaboxInitAction> = listOf(
        ParaboxInitAction.TextInputAction(
            key = "host",
            title = "输入正向 WebSocket 服务地址",
            description = "请输入 OneBot 正向 WebSocket 服务地址",
            label = "IP 地址",
            type = ParaboxInitAction.KeyboardType.NUMBER,
        ),
        ParaboxInitAction.TextInputAction(
            key = "port",
            title = "输入 OneBot 服务端口",
            description = "请输入 OneBot 正向 WebSocket 服务端口",
            label = "端口",
            type = ParaboxInitAction.KeyboardType.NUMBER,
        ),
        ParaboxInitAction.TextInputAction(
            key = "token",
            title = "输入 AccessToken",
            description = "请输入 Access Token（可留空）",
            label = "Access Token",
            type = ParaboxInitAction.KeyboardType.TEXT,
        ),
    )

    override suspend fun onInitStep(
        wizardSessionId: String,
        currentStepIndex: Int,
        submittedKey: String?,
        submittedValue: Any?,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        if (!accumulatedExtra.has("compatibility_mode")) {
            accumulatedExtra.put("compatibility_mode", false)
        }
        if (currentStepIndex < 0) {
            return ParaboxInitStepResult(
                steps = initSteps,
                nextStepIndex = 0,
                updatedExtra = accumulatedExtra,
            )
        }

        val key = submittedKey ?: initSteps.getOrNull(currentStepIndex)?.key.orEmpty()
        val validation = validateConfig(key, submittedValue, accumulatedExtra)
        if (validation is ParaboxInitActionResult.Error) {
            return ParaboxInitStepResult(
                steps = initSteps,
                nextStepIndex = currentStepIndex,
                validationError = validation.message,
                updatedExtra = accumulatedExtra,
            )
        }

        accumulatedExtra.put(key, submittedValue?.toString().orEmpty())
        val nextIndex = currentStepIndex + 1
        return ParaboxInitStepResult(
            steps = initSteps,
            nextStepIndex = nextIndex,
            completed = nextIndex >= initSteps.size,
            updatedExtra = accumulatedExtra,
        )
    }

    override suspend fun getConfigItems(currentExtra: JSONObject): List<ParaboxConfigItem> = listOf(
        ParaboxConfigItem.Category(
            key = "category_general",
            title = "通用",
            description = "通用配置",
        ),
        ParaboxConfigItem.SwitchConfigItem(
            key = "auto_reconnect",
            title = "自动重连",
            description = "连接断开后自动重连",
            defaultValue = true,
        ),
        ParaboxConfigItem.TextInputConfigItem(
            key = "host",
            title = "OneBot 服务地址",
            description = "OneBot 正向 WebSocket 服务地址",
            label = "IP 地址",
            type = ParaboxInitAction.KeyboardType.NUMBER,
            defaultValue = null,
        ),
        ParaboxConfigItem.TextInputConfigItem(
            key = "port",
            title = "OneBot 服务端口",
            description = "OneBot 正向 WebSocket 服务端口",
            label = "端口",
            type = ParaboxInitAction.KeyboardType.NUMBER,
            defaultValue = null,
        ),
        ParaboxConfigItem.TextInputConfigItem(
            key = "token",
            title = "AccessToken",
            description = "Access Token（可留空）",
            label = "Token",
            defaultValue = null,
        ),
        ParaboxConfigItem.TextInputConfigItem(
            key = "heart_interval",
            title = "心跳间隔",
            description = "控制心跳包的发送时间间隔，单位为秒",
            label = "时间",
            type = ParaboxInitAction.KeyboardType.NUMBER,
            defaultValue = "10",
        ),
        ParaboxConfigItem.Category(
            key = "category_advanced",
            title = "高级",
            description = "高级配置",
        ),
        ParaboxConfigItem.SwitchConfigItem(
            key = "compatibility_mode",
            title = "兼容模式",
            description = "启用协议之外的兼容逻辑",
            defaultValue = false,
        ),
    )

    override suspend fun validateConfig(
        configKey: String,
        value: Any?,
        currentExtra: JSONObject,
    ): ParaboxInitActionResult {
        val str = value?.toString().orEmpty()
        return when (configKey) {
            "host" -> {
                if (str.matches(IPV4_REGEX) || str.equals("localhost", ignoreCase = true)) {
                    ParaboxInitActionResult.Done
                } else {
                    ParaboxInitActionResult.Error("请输入正确的 IP 地址")
                }
            }
            "port" -> {
                val port = str.toIntOrNull()
                if (port != null && port in 1..65535) {
                    ParaboxInitActionResult.Done
                } else {
                    ParaboxInitActionResult.Error("请输入正确的端口（1-65535）")
                }
            }
            "heart_interval" -> {
                val sec = str.toIntOrNull()
                if (sec != null && sec in 1..60) {
                    ParaboxInitActionResult.Done
                } else {
                    ParaboxInitActionResult.Error("输入时间超出允许范围（1-60）")
                }
            }
            else -> ParaboxInitActionResult.Done
        }
    }

    override fun createSession(
        sessionId: String,
        extra: JSONObject,
        hostPackageName: String,
    ): ParaboxSession = OneBot11Connection(
        sessionId = sessionId,
        extra = extra,
        hostPackageName = hostPackageName,
    )

    companion object {
        private val IPV4_REGEX = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")
    }
}
