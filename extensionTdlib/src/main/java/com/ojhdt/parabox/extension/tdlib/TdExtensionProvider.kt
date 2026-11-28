package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountMeta
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdErrorException
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.TdProxyProtocol
import com.ojhdt.parabox.extension.tdlib.core.send
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdt.parabox.extension.tdlib.mapper.TdMessageMapper
import com.ojhdtapp.parabox.protocol.v2.ExtensionCapability
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionDescriptor
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionProvider
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxInitStepResult
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TdExtensionProvider(
    private val coordinator: TdClientCoordinator,
    private val mediaBridge: TdMediaBridge,
    private val authWaitTimeoutMs: Long = 25_000L,
) : ParaboxExtensionProvider {

    private val wizardPasswordHints = ConcurrentHashMap<String, String>()

    override val descriptor = ParaboxExtensionDescriptor(
        key = EXTENSION_KEY,
        name = "Telegram (TDLib)",
        description = "支持多账号并行的官方 TDLib 独立进程扩展",
        capabilities = setOf(
            ExtensionCapability.CAPABILITY_RECALL_MESSAGE,
            ExtensionCapability.CAPABILITY_QUERY_HISTORY,
            ExtensionCapability.CAPABILITY_STREAM_RESOURCE,
        ),
    )

    override suspend fun onInitStep(
        wizardSessionId: String,
        currentStepIndex: Int,
        submittedKey: String?,
        submittedValue: Any?,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        val accountId = accumulatedExtra.optString(KEY_ACCOUNT_ID).ifBlank {
            "tg_${UUID.randomUUID().toString().replace("-", "").take(12)}".also {
                accumulatedExtra.put(KEY_ACCOUNT_ID, it)
            }
        }
        if (!accumulatedExtra.has(KEY_AUTO_DOWNLOAD_MEDIA)) {
            accumulatedExtra.put(KEY_AUTO_DOWNLOAD_MEDIA, true)
        }
        if (!accumulatedExtra.has(KEY_MAX_DOWNLOAD_MB)) {
            accumulatedExtra.put(KEY_MAX_DOWNLOAD_MB, DEFAULT_MAX_DOWNLOAD_MB.toString())
        }
        if (!accumulatedExtra.has(KEY_SUPERGROUP_PERIODIC_SYNC)) {
            accumulatedExtra.put(KEY_SUPERGROUP_PERIODIC_SYNC, true)
        }

        val currentSteps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
        if (currentStepIndex < 0) {
            return ParaboxInitStepResult(
                steps = currentSteps,
                nextStepIndex = 0,
                updatedExtra = accumulatedExtra,
            )
        }

        val stepKey = submittedKey ?: currentSteps.getOrNull(currentStepIndex)?.key.orEmpty()
        return when (stepKey) {
            TdProxyConfig.KEY_ENABLED -> {
                val enabled = submittedValue as? Boolean ?: submittedValue?.toString().toBoolean()
                accumulatedExtra.put(TdProxyConfig.KEY_ENABLED, enabled)
                val updatedSteps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = currentStepIndex + 1,
                    updatedExtra = accumulatedExtra,
                )
            }

            TdProxyConfig.KEY_TYPE -> {
                val protocol = when (submittedValue) {
                    is Int -> TdProxyProtocol.fromIndex(submittedValue)
                    else -> TdProxyProtocol.fromString(submittedValue?.toString())
                }
                accumulatedExtra.put(TdProxyConfig.KEY_TYPE, protocol.label)
                val updatedSteps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = currentStepIndex + 1,
                    updatedExtra = accumulatedExtra,
                )
            }

            TdProxyConfig.KEY_HOST,
            TdProxyConfig.KEY_PORT,
            TdProxyConfig.KEY_USERNAME,
            TdProxyConfig.KEY_PASSWORD -> {
                val validation = validateConfig(stepKey, submittedValue, accumulatedExtra)
                if (validation is ParaboxInitActionResult.Error) {
                    return ParaboxInitStepResult(
                        steps = currentSteps,
                        nextStepIndex = currentStepIndex,
                        validationError = validation.message,
                        updatedExtra = accumulatedExtra,
                    )
                }
                accumulatedExtra.put(stepKey, submittedValue?.toString().orEmpty().trim())
                val updatedSteps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = currentStepIndex + 1,
                    updatedExtra = accumulatedExtra,
                )
            }

            KEY_PHONE_NUMBER -> handlePhoneSubmit(
                wizardSessionId = wizardSessionId,
                accountId = accountId,
                currentStepIndex = currentStepIndex,
                rawPhone = submittedValue?.toString().orEmpty().trim(),
                accumulatedExtra = accumulatedExtra,
            )

            KEY_AUTH_CODE -> handleCodeSubmit(
                wizardSessionId = wizardSessionId,
                accountId = accountId,
                currentStepIndex = currentStepIndex,
                rawCode = submittedValue?.toString().orEmpty().trim(),
                accumulatedExtra = accumulatedExtra,
            )

            KEY_TWO_FA_PASSWORD -> handlePasswordSubmit(
                wizardSessionId = wizardSessionId,
                accountId = accountId,
                currentStepIndex = currentStepIndex,
                password = submittedValue?.toString().orEmpty(),
                accumulatedExtra = accumulatedExtra,
            )

            else -> ParaboxInitStepResult(
                steps = currentSteps,
                nextStepIndex = currentStepIndex + 1,
                completed = currentStepIndex + 1 >= currentSteps.size,
                updatedExtra = accumulatedExtra,
            )
        }
    }

    private suspend fun handlePhoneSubmit(
        wizardSessionId: String,
        accountId: String,
        currentStepIndex: Int,
        rawPhone: String,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        val steps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
        if (rawPhone.isBlank() || !rawPhone.matches(PHONE_REGEX)) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "请输入包含国家区号的手机号（例如 +8613800138000）",
                updatedExtra = accumulatedExtra,
            )
        }

        val proxyConfig = TdProxyConfig.fromExtra(accumulatedExtra)
        val client = try {
            coordinator.getOrCreateWizardClient(wizardSessionId, accountId, proxyConfig)
        } catch (e: Exception) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "启动 TDLib 客户端失败：${e.message}",
                updatedExtra = accumulatedExtra,
            )
        }

        val readyForPhone = awaitAuthState(client) { state ->
            state is TdApi.AuthorizationStateWaitPhoneNumber ||
                state is TdApi.AuthorizationStateWaitCode ||
                state is TdApi.AuthorizationStateWaitPassword ||
                state is TdApi.AuthorizationStateReady
        }
        if (readyForPhone == null) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "TDLib 初始化超时，请检查网络或代理设置后重试",
                updatedExtra = accumulatedExtra,
            )
        }

        if (readyForPhone is TdApi.AuthorizationStateReady) {
            return finalizeWizardAccount(wizardSessionId, accountId, client, accumulatedExtra)
        }

        if (readyForPhone is TdApi.AuthorizationStateWaitPhoneNumber) {
            try {
                client.send<TdApi.Ok>(TdApi.SetAuthenticationPhoneNumber(rawPhone, null))
            } catch (e: TdErrorException) {
                return ParaboxInitStepResult(
                    steps = steps,
                    nextStepIndex = currentStepIndex,
                    validationError = "手机号验证失败：${e.message}",
                    updatedExtra = accumulatedExtra,
                )
            } catch (e: Exception) {
                return ParaboxInitStepResult(
                    steps = steps,
                    nextStepIndex = currentStepIndex,
                    validationError = "发送验证码失败：${e.message}",
                    updatedExtra = accumulatedExtra,
                )
            }
        }

        val nextState = awaitAuthState(client) { state ->
            state is TdApi.AuthorizationStateWaitCode ||
                state is TdApi.AuthorizationStateWaitPassword ||
                state is TdApi.AuthorizationStateReady
        }
        accumulatedExtra.put(KEY_PHONE_NUMBER, rawPhone)

        return when (nextState) {
            is TdApi.AuthorizationStateReady -> finalizeWizardAccount(wizardSessionId, accountId, client, accumulatedExtra)
            is TdApi.AuthorizationStateWaitPassword -> {
                wizardPasswordHints[wizardSessionId] = nextState.passwordHint.orEmpty()
                val updatedSteps = buildSteps(accumulatedExtra, nextState.passwordHint.orEmpty())
                val pwdIndex = updatedSteps.indexOfFirst { it.key == KEY_TWO_FA_PASSWORD }.coerceAtLeast(currentStepIndex + 1)
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = pwdIndex,
                    updatedExtra = accumulatedExtra,
                )
            }
            is TdApi.AuthorizationStateWaitCode -> {
                val updatedSteps = buildSteps(accumulatedExtra, null)
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = currentStepIndex + 1,
                    updatedExtra = accumulatedExtra,
                )
            }
            else -> ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "等待 Telegram 返回验证码状态超时，请检查代理连通性",
                updatedExtra = accumulatedExtra,
            )
        }
    }

    private suspend fun handleCodeSubmit(
        wizardSessionId: String,
        accountId: String,
        currentStepIndex: Int,
        rawCode: String,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        val steps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
        if (rawCode.isBlank() || !rawCode.all { it.isDigit() }) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "请输入正确的数字验证码",
                updatedExtra = accumulatedExtra,
            )
        }

        val client = coordinator.getWizardClient(wizardSessionId)
            ?: return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "登录会话已过期，请返回上一步重新发送验证码",
                updatedExtra = accumulatedExtra,
            )

        if (client.authorizationState.value is TdApi.AuthorizationStateWaitCode) {
            try {
                client.send<TdApi.Ok>(TdApi.CheckAuthenticationCode(rawCode))
            } catch (e: TdErrorException) {
                return ParaboxInitStepResult(
                    steps = steps,
                    nextStepIndex = currentStepIndex,
                    validationError = "验证码错误：${e.message}",
                    updatedExtra = accumulatedExtra,
                )
            } catch (e: Exception) {
                return ParaboxInitStepResult(
                    steps = steps,
                    nextStepIndex = currentStepIndex,
                    validationError = "校验验证码失败：${e.message}",
                    updatedExtra = accumulatedExtra,
                )
            }
        }

        val nextState = awaitAuthState(client) { state ->
            state is TdApi.AuthorizationStateReady || state is TdApi.AuthorizationStateWaitPassword
        }
        return when (nextState) {
            is TdApi.AuthorizationStateReady -> finalizeWizardAccount(wizardSessionId, accountId, client, accumulatedExtra)
            is TdApi.AuthorizationStateWaitPassword -> {
                val hint = nextState.passwordHint.orEmpty()
                wizardPasswordHints[wizardSessionId] = hint
                val updatedSteps = buildSteps(accumulatedExtra, hint)
                ParaboxInitStepResult(
                    steps = updatedSteps,
                    nextStepIndex = currentStepIndex + 1,
                    updatedExtra = accumulatedExtra,
                )
            }
            else -> ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "验证码校验超时，请重试",
                updatedExtra = accumulatedExtra,
            )
        }
    }

    private suspend fun handlePasswordSubmit(
        wizardSessionId: String,
        accountId: String,
        currentStepIndex: Int,
        password: String,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        val steps = buildSteps(accumulatedExtra, wizardPasswordHints[wizardSessionId])
        if (password.isBlank()) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "两步验证密码不能为空",
                updatedExtra = accumulatedExtra,
            )
        }
        val client = coordinator.getWizardClient(wizardSessionId)
            ?: return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "登录会话已过期，请返回重试",
                updatedExtra = accumulatedExtra,
            )

        try {
            client.send<TdApi.Ok>(TdApi.CheckAuthenticationPassword(password))
        } catch (e: TdErrorException) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "两步验证密码错误：${e.message}",
                updatedExtra = accumulatedExtra,
            )
        } catch (e: Exception) {
            return ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "校验两步验证失败：${e.message}",
                updatedExtra = accumulatedExtra,
            )
        }

        val ready = awaitAuthState(client) { it is TdApi.AuthorizationStateReady }
        return if (ready is TdApi.AuthorizationStateReady) {
            finalizeWizardAccount(wizardSessionId, accountId, client, accumulatedExtra)
        } else {
            ParaboxInitStepResult(
                steps = steps,
                nextStepIndex = currentStepIndex,
                validationError = "等待 Telegram 授权完成超时，请重试",
                updatedExtra = accumulatedExtra,
            )
        }
    }

    private suspend fun finalizeWizardAccount(
        wizardSessionId: String,
        accountId: String,
        client: TdClientApi,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult {
        val me = runCatching { client.send<TdApi.User>(TdApi.GetMe()) }.getOrNull()
        val displayName = me?.let { TdMessageMapper.formatUserDisplayName(it) }
            ?: accumulatedExtra.optString(KEY_PHONE_NUMBER, accountId)
        val phone = me?.phoneNumber?.ifBlank { null } ?: accumulatedExtra.optString(KEY_PHONE_NUMBER)
        val username = me?.usernames?.editableUsername.orEmpty()
        val userId = me?.id ?: 0L

        coordinator.storage.saveMeta(
            TdAccountMeta(
                accountId = accountId,
                userId = userId,
                displayName = displayName,
                phoneNumber = phone,
                username = username,
            )
        )
        accumulatedExtra.put(KEY_ACCOUNT_USER_ID, userId)
        accumulatedExtra.put(KEY_ACCOUNT_DISPLAY_NAME, displayName)
        if (phone.isNotBlank()) accumulatedExtra.put(KEY_PHONE_NUMBER, phone)

        // Release DB lock cleanly before host opens TdSession for this accountId
        wizardPasswordHints.remove(wizardSessionId)
        coordinator.closeWizardClient(wizardSessionId)

        val finalSteps = buildSteps(accumulatedExtra, null)
        return ParaboxInitStepResult(
            steps = finalSteps,
            nextStepIndex = finalSteps.size,
            completed = true,
            updatedExtra = accumulatedExtra,
        )
    }

    private suspend fun awaitAuthState(
        client: TdClientApi,
        predicate: (TdApi.AuthorizationState) -> Boolean,
    ): TdApi.AuthorizationState? {
        val current = client.authorizationState.value
        if (current != null && predicate(current)) return current
        return withTimeoutOrNull(authWaitTimeoutMs) {
            client.authorizationState.first { it != null && predicate(it) }
        }
    }

    private fun buildSteps(extra: JSONObject, passwordHint: String?): List<ParaboxInitAction> = buildList {
        val proxyEnabled = extra.optBoolean(TdProxyConfig.KEY_ENABLED, false)
        add(
            ParaboxInitAction.SwitchAction(
                key = TdProxyConfig.KEY_ENABLED,
                title = "启用网络代理",
                description = "若所在网络无法直连 Telegram，请开启后配置 SOCKS5 / HTTP / MTProto 代理",
                defaultValue = proxyEnabled,
            )
        )
        if (proxyEnabled) {
            val selectedProtocol = TdProxyProtocol.fromString(extra.optString(TdProxyConfig.KEY_TYPE))
            add(
                ParaboxInitAction.SelectAction(
                    key = TdProxyConfig.KEY_TYPE,
                    title = "选择代理协议",
                    description = "支持 SOCKS5、HTTP 与 MTProto 代理",
                    options = TdProxyProtocol.labels,
                    defaultValue = selectedProtocol.ordinal,
                )
            )
            add(
                ParaboxInitAction.TextInputAction(
                    key = TdProxyConfig.KEY_HOST,
                    title = "代理服务器地址",
                    description = "输入代理服务器 IP 或域名",
                    label = "服务器地址",
                    defaultValue = extra.optString(TdProxyConfig.KEY_HOST, "127.0.0.1"),
                )
            )
            add(
                ParaboxInitAction.TextInputAction(
                    key = TdProxyConfig.KEY_PORT,
                    title = "代理端口",
                    description = "输入代理服务器端口（1-65535）",
                    label = "端口",
                    type = ParaboxInitAction.KeyboardType.NUMBER,
                    defaultValue = extra.optString(TdProxyConfig.KEY_PORT, "7890"),
                )
            )
            add(
                ParaboxInitAction.TextInputAction(
                    key = TdProxyConfig.KEY_USERNAME,
                    title = if (selectedProtocol == TdProxyProtocol.MTPROTO) "MTProto Secret" else "代理用户名",
                    description = if (selectedProtocol == TdProxyProtocol.MTPROTO) {
                        "输入 MTProto 代理密钥（必填）"
                    } else {
                        "无鉴权可留空"
                    },
                    label = if (selectedProtocol == TdProxyProtocol.MTPROTO) "Secret" else "用户名",
                    defaultValue = extra.optString(TdProxyConfig.KEY_USERNAME, ""),
                )
            )
            if (selectedProtocol != TdProxyProtocol.MTPROTO) {
                add(
                    ParaboxInitAction.TextInputAction(
                        key = TdProxyConfig.KEY_PASSWORD,
                        title = "代理密码",
                        description = "无鉴权可留空",
                        label = "密码",
                        type = ParaboxInitAction.KeyboardType.PASSWORD,
                        defaultValue = extra.optString(TdProxyConfig.KEY_PASSWORD, ""),
                    )
                )
            }
        }
        add(
            ParaboxInitAction.TextInputAction(
                key = KEY_PHONE_NUMBER,
                title = "输入 Telegram 手机号",
                description = "请输入带国际区号的手机号（例如 +8613800138000）；每个新连接对应独立账号会话",
                label = "手机号",
                type = ParaboxInitAction.KeyboardType.TEXT,
                defaultValue = extra.optString(KEY_PHONE_NUMBER, "+"),
            )
        )
        add(
            ParaboxInitAction.TextInputAction(
                key = KEY_AUTH_CODE,
                title = "输入验证码",
                description = "请输入其他已登录 Telegram 客户端或短信收到的数字验证码",
                label = "验证码",
                type = ParaboxInitAction.KeyboardType.NUMBER,
            )
        )
        if (passwordHint != null) {
            val hintDesc = if (passwordHint.isNotBlank()) {
                "该账号已开启两步验证（密码提示：$passwordHint）"
            } else {
                "该账号已开启两步验证，请输入两步验证密码"
            }
            add(
                ParaboxInitAction.TextInputAction(
                    key = KEY_TWO_FA_PASSWORD,
                    title = "输入两步验证密码",
                    description = hintDesc,
                    label = "两步验证密码",
                    type = ParaboxInitAction.KeyboardType.PASSWORD,
                )
            )
        }
    }

    override suspend fun getConfigItems(currentExtra: JSONObject): List<ParaboxConfigItem> {
        val accountId = currentExtra.optString(KEY_ACCOUNT_ID)
        val displayName = currentExtra.optString(KEY_ACCOUNT_DISPLAY_NAME).ifBlank {
            coordinator.storage.readMeta(accountId)?.displayName.orEmpty()
        }
        val phone = currentExtra.optString(KEY_PHONE_NUMBER)
        val accountSummary = buildString {
            if (displayName.isNotBlank()) append(displayName)
            if (phone.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(phone)
            }
            if (accountId.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(accountId)
            }
        }.ifBlank { "Telegram TDLib 账号" }

        return listOf(
            ParaboxConfigItem.Category("cat_account", "账号与媒体", accountSummary),
            ParaboxConfigItem.SwitchConfigItem(
                key = KEY_AUTO_DOWNLOAD_MEDIA,
                title = "自动下载消息媒体",
                description = "接收图片、语音、视频和文件时自动下载并通过 FileProvider 共享给宿主",
                defaultValue = true,
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = KEY_MAX_DOWNLOAD_MB,
                title = "自动下载大小上限 (MB)",
                description = "超过该体积的媒体文件将跳过自动下载（1-500）",
                label = "上限 (MB)",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                defaultValue = DEFAULT_MAX_DOWNLOAD_MB.toString(),
            ),
            ParaboxConfigItem.Category("cat_sync", "消息同步", "调整超级群与频道的增量同步策略"),
            ParaboxConfigItem.SwitchConfigItem(
                key = KEY_SUPERGROUP_PERIODIC_SYNC,
                title = "轮转超级群/频道增量同步",
                description = "前台运行时定期低频探测未打开的超级群与频道（后台自动暂停以降低耗电）",
                defaultValue = true,
            ),
            ParaboxConfigItem.Category("cat_proxy", "网络代理", "修改代理后保存将自动重启当前账号连接"),
            ParaboxConfigItem.SwitchConfigItem(
                key = TdProxyConfig.KEY_ENABLED,
                title = "启用代理",
                description = "仅对当前 Telegram 账号连接生效",
                defaultValue = false,
            ),
            ParaboxConfigItem.SelectConfigItem(
                key = TdProxyConfig.KEY_TYPE,
                title = "代理协议",
                description = "SOCKS5 / HTTP / MTProto",
                options = TdProxyProtocol.labels,
                defaultValue = 0,
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = TdProxyConfig.KEY_HOST,
                title = "代理地址",
                description = "代理服务器 IP 或域名",
                label = "地址",
                defaultValue = "127.0.0.1",
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = TdProxyConfig.KEY_PORT,
                title = "代理端口",
                description = "1-65535",
                label = "端口",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                defaultValue = "7890",
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = TdProxyConfig.KEY_USERNAME,
                title = "代理用户名 / MTProto Secret",
                description = "无鉴权可留空",
                label = "用户名 / Secret",
                defaultValue = "",
            ),
            ParaboxConfigItem.TextInputConfigItem(
                key = TdProxyConfig.KEY_PASSWORD,
                title = "代理密码",
                description = "无鉴权可留空",
                label = "密码",
                type = ParaboxInitAction.KeyboardType.PASSWORD,
                defaultValue = "",
            ),
            ParaboxConfigItem.Category("cat_actions", "账号操作", accountSummary),
            ParaboxConfigItem.ActionConfigItem(
                key = ACTION_CLEAR_CACHE,
                title = "清理该账号媒体缓存",
                description = "释放当前连接下载的图片与文件缓存，不影响登录状态及其他账号",
                confirmModel = ParaboxConfigItem.ActionConfigItem.ConfirmModel(
                    title = "清理媒体缓存",
                    description = "确定要清理账号 $accountSummary 的本地媒体缓存吗？",
                    confirmText = "清理",
                    cancelText = "取消",
                ),
            ),
            ParaboxConfigItem.ActionConfigItem(
                key = ACTION_LOGOUT,
                title = "登出当前账号",
                description = "仅登出当前连接对应的 Telegram 账号并删除本地数据库，不影响其他已登录账号",
                confirmModel = ParaboxConfigItem.ActionConfigItem.ConfirmModel(
                    title = "登出 Telegram 账号",
                    description = "确定要登出 $accountSummary 吗？登出后该连接需要删除并重新建立。",
                    confirmText = "确定登出",
                    cancelText = "取消",
                ),
            ),
        )
    }

    override suspend fun validateConfig(
        configKey: String,
        value: Any?,
        currentExtra: JSONObject,
    ): ParaboxInitActionResult {
        val str = value?.toString().orEmpty().trim()
        return when (configKey) {
            TdProxyConfig.KEY_HOST -> {
                if (str.isBlank()) ParaboxInitActionResult.Error("代理地址不能为空") else ParaboxInitActionResult.Done
            }
            TdProxyConfig.KEY_PORT -> {
                val port = str.toIntOrNull()
                if (port != null && port in 1..65535) {
                    ParaboxInitActionResult.Done
                } else {
                    ParaboxInitActionResult.Error("请输入正确的代理端口（1-65535）")
                }
            }
            TdProxyConfig.KEY_USERNAME -> {
                val proto = TdProxyProtocol.fromString(currentExtra.optString(TdProxyConfig.KEY_TYPE))
                if (proto == TdProxyProtocol.MTPROTO && str.isBlank()) {
                    ParaboxInitActionResult.Error("MTProto 代理必须填写 Secret")
                } else {
                    ParaboxInitActionResult.Done
                }
            }
            KEY_MAX_DOWNLOAD_MB -> {
                val mb = str.toIntOrNull()
                if (mb != null && mb in 1..500) {
                    ParaboxInitActionResult.Done
                } else {
                    ParaboxInitActionResult.Error("请输入 1 到 500 之间的数字")
                }
            }
            else -> ParaboxInitActionResult.Done
        }
    }

    override suspend fun onConfigAction(
        actionKey: String,
        sessionId: String,
        currentExtra: JSONObject,
    ): ParaboxResult {
        val accountId = currentExtra.optString(KEY_ACCOUNT_ID)
        if (accountId.isBlank()) {
            return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "缺少账号标识 account_id")
        }
        return when (actionKey) {
            ACTION_CLEAR_CACHE -> {
                val freedBytes = coordinator.storage.clearMediaCache(accountId)
                val mb = String.format("%.2f", freedBytes / (1024.0 * 1024.0))
                ParaboxResult(ParaboxResult.SUCCESS, "已清理 $mb MB 媒体缓存")
            }
            ACTION_LOGOUT -> {
                coordinator.logoutAndPurgeAccount(accountId)
                ParaboxResult(ParaboxResult.SUCCESS, "已登出当前 Telegram 账号")
            }
            else -> ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "不支持的操作：$actionKey")
        }
    }

    override fun createSession(
        sessionId: String,
        extra: JSONObject,
        hostPackageName: String,
    ): ParaboxSession = TdSession(
        sessionId = sessionId,
        extra = extra,
        hostPackageName = hostPackageName,
        coordinator = coordinator,
        mediaBridge = mediaBridge,
    )

    companion object {
        const val EXTENSION_KEY = "telegram_tdlib"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_ACCOUNT_USER_ID = "account_user_id"
        const val KEY_ACCOUNT_DISPLAY_NAME = "account_display_name"
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_AUTH_CODE = "auth_code"
        const val KEY_TWO_FA_PASSWORD = "two_fa_password"
        const val KEY_AUTO_DOWNLOAD_MEDIA = "auto_download_media"
        const val KEY_MAX_DOWNLOAD_MB = "max_download_mb"
        const val KEY_SUPERGROUP_PERIODIC_SYNC = "supergroup_periodic_sync"
        const val DEFAULT_MAX_DOWNLOAD_MB = 20

        const val ACTION_CLEAR_CACHE = "clear_cache"
        const val ACTION_LOGOUT = "logout"

        private val PHONE_REGEX = Regex("^\\+?[0-9\\s-]{6,20}$")
    }
}
