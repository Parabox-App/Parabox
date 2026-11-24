package com.ojhdt.parabox.extension.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.ojhdtapp.parabox.protocol.v2.ExtensionCapability
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionDescriptor
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxInitStepResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxMediaHelper
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DemoExtensionProvider(
    private val appContext: Context,
) : ParaboxExtensionProvider {

    override val descriptor = ParaboxExtensionDescriptor(
        key = "demo_echo_v2",
        name = "Demo Echo (PEP v2)",
        description = "独立进程 gRPC-Binder 回声与媒体传输测试扩展",
        capabilities = setOf(ExtensionCapability.CAPABILITY_RECALL_MESSAGE),
    )

    private val initSteps = listOf(
        ParaboxInitAction.TextInputAction(
            key = "bot_name",
            title = "设置回声机器人名称",
            description = "用于显示在聊天中的发送者昵称",
            label = "机器人名称",
            defaultValue = "PEP v2 Echo Bot",
        ),
        ParaboxInitAction.SwitchAction(
            key = "echo_image",
            title = "附带跨进程测试图片",
            description = "开启后回复消息时通过 FileProvider 跨进程附带一张测试图片",
            defaultValue = false,
        ),
    )

    override suspend fun onInitStep(
        wizardSessionId: String,
        currentStepIndex: Int,
        submittedKey: String?,
        submittedValue: Any?,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
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
        when (submittedValue) {
            is Boolean -> accumulatedExtra.put(key, submittedValue)
            else -> accumulatedExtra.put(key, submittedValue?.toString().orEmpty())
        }
        val next = currentStepIndex + 1
        return ParaboxInitStepResult(
            steps = initSteps,
            nextStepIndex = next,
            completed = next >= initSteps.size,
            updatedExtra = accumulatedExtra,
        )
    }

    override suspend fun getConfigItems(currentExtra: JSONObject): List<ParaboxConfigItem> = listOf(
        ParaboxConfigItem.Category("cat_demo", "Demo 设置", "PEP v2 独立进程扩展配置"),
        ParaboxConfigItem.TextInputConfigItem(
            key = "bot_name",
            title = "机器人名称",
            description = "回声消息发送者显示名称",
            label = "名称",
            defaultValue = "PEP v2 Echo Bot",
        ),
        ParaboxConfigItem.SwitchConfigItem(
            key = "echo_image",
            title = "附带测试图片",
            description = "测试跨进程 FileProvider URI 授权",
            defaultValue = false,
        ),
    )

    override suspend fun validateConfig(
        configKey: String,
        value: Any?,
        currentExtra: JSONObject,
    ): ParaboxInitActionResult {
        return when (configKey) {
            "bot_name" -> {
                if (value?.toString().orEmpty().trim().isEmpty()) {
                    ParaboxInitActionResult.Error("机器人名称不能为空")
                } else {
                    ParaboxInitActionResult.Done
                }
            }
            else -> ParaboxInitActionResult.Done
        }
    }

    override fun createSession(
        sessionId: String,
        extra: JSONObject,
        hostPackageName: String,
    ): ParaboxSession = DemoEchoSession(appContext, sessionId, extra, hostPackageName)
}

class DemoEchoSession(
    private val appContext: Context,
    sessionId: String,
    extra: JSONObject,
    hostPackageName: String,
) : ParaboxSession(sessionId, extra, hostPackageName) {

    private val botName: String
        get() = extra.optString("bot_name", "PEP v2 Echo Bot").ifBlank { "PEP v2 Echo Bot" }

    override suspend fun onInitialize(): Boolean {
        updateStatus(ParaboxConnectionStatus.Active)
        // Push an initial greeting message so the chat immediately appears in Parabox
        sessionScope.launch {
            delay(500)
            emitMessage(
                ReceiveMessage(
                    contents = listOf(
                        ParaboxPlainText("已通过独立进程 gRPC-Binder 连接成功（PID=${android.os.Process.myPid()}）。发送任意文本测试回声。")
                    ),
                    sender = ParaboxContact(
                        basicInfo = ParaboxBasicInfo(botName, ParaboxResourceInfo.ParaboxEmptyInfo),
                        uid = "demo_bot",
                    ),
                    chat = ParaboxChat(
                        basicInfo = ParaboxBasicInfo(botName, ParaboxResourceInfo.ParaboxEmptyInfo),
                        type = ParaboxChat.TYPE_PRIVATE,
                        uid = "demo_chat",
                    ),
                    timestamp = System.currentTimeMillis(),
                    uuid = "welcome-${UUID.randomUUID()}",
                )
            )
        }
        return true
    }

    override suspend fun onSendMessage(message: SendMessage): ParaboxResult {
        val text = message.contents.joinToString(" ") { it.contentToString() }
        val replyElements = buildList {
            add(ParaboxPlainText("[$botName | PID=${android.os.Process.myPid()}] Echo: $text"))
            if (extra.optBoolean("echo_image", false)) {
                createSampleImageFile()?.let { file ->
                    val uri = ParaboxMediaHelper.shareFileWithHost(
                        context = appContext,
                        authority = "${appContext.packageName}.fileprovider",
                        file = file,
                        hostPackageName = hostPackageName,
                    )
                    add(
                        ParaboxImage(
                            width = 128,
                            height = 128,
                            fileName = file.name,
                            resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri),
                        )
                    )
                }
            }
        }
        sessionScope.launch {
            emitMessage(
                ReceiveMessage(
                    contents = replyElements,
                    sender = ParaboxContact(
                        basicInfo = ParaboxBasicInfo(botName, ParaboxResourceInfo.ParaboxEmptyInfo),
                        uid = "demo_bot",
                    ),
                    chat = message.chat,
                    timestamp = System.currentTimeMillis(),
                    uuid = "echo-${UUID.randomUUID()}",
                )
            )
        }
        return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
    }

    override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo {
        return ParaboxBasicInfo(botName, ParaboxResourceInfo.ParaboxEmptyInfo)
    }

    private fun createSampleImageFile(): File? = runCatching {
        val file = File(appContext.cacheDir, "demo_echo_sample.png")
        if (!file.exists()) {
            val bmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.rgb(66, 133, 244))
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
        }
        file
    }.getOrNull()
}
