package com.ojhdtapp.parabox.domain.built_in.td

import android.os.Build
import android.util.Log
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.domain.built_in.td.model.AuthorizationState
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TdInitHandler : ParaboxInitHandler(), Client.ResultHandler, Client.ExceptionHandler {
    private val client: Client by lazy {
        Client.create(this, this, this)
    }

    private var authorizationState: AuthorizationState = AuthorizationState.WaitTdlibParameter
    private var authorizationWaitTdLibCot: Continuation<ParaboxInitActionResult>? = null
    private var authorizationWaitPhoneNumberCot: Continuation<ParaboxInitActionResult>? = null
    private var authorizationWaitCodeCot: Continuation<ParaboxInitActionResult>? = null
    private var authorizationWaitPasswordCot: Continuation<ParaboxInitActionResult>? = null

    override fun onInit() {
        client.send(TdApi.SetLogVerbosityLevel(1), this)
        client.send(TdApi.SetDatabaseEncryptionKey(), this)
    }

    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        Log.d("tdlib", "getInitAction, currentActionIndex=$currentActionIndex")
        return listOf(
            ParaboxInitAction.LoadingAction(
                key= "init",
                title = "初始化TDLib",
                errMsg = "",
                description = "等待TdLib完成启动前验证流程",
                onSkipCheck = {
                    authorizationState.ordinal > AuthorizationState.WaitTdlibParameter.ordinal
                },
                onResult = {
                    Log.i("tdlib", "getInitAction, init=$authorizationState")
                    if (authorizationState.ordinal < AuthorizationState.WaitPhoneNumber.ordinal) {
                        try {
                            withTimeout(5000) {
                                suspendCoroutine<ParaboxInitActionResult> { cot ->
                                    authorizationWaitTdLibCot = cot
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            authorizationWaitTdLibCot = null
                            ParaboxInitActionResult.Error("初始化超时，请重试")
                        }
                    } else {
                        authorizationWaitTdLibCot = null
                        ParaboxInitActionResult.Done
                    }
                }
            ),
            ParaboxInitAction.PhoneInputAction(
                key = "phone_number",
                title = "输入电话号码",
                errMsg = "",
                description = "请输入与Telegram账号绑定的电话号码",
                onSkipCheck = {
                    authorizationState.ordinal > AuthorizationState.WaitPhoneNumber.ordinal
                },
                onResult = { res: String ->
                    Log.i("tdlib", "getInitAction, phone_number=$res, state=$authorizationState")
                    if (res.isBlank()) {
                        ParaboxInitActionResult.Error("手机号不能为空")
                    } else {
                        if (authorizationState <= AuthorizationState.WaitPhoneNumber) {
                            suspendCoroutine<ParaboxInitActionResult> { cot ->
                                authorizationWaitPhoneNumberCot = cot
                                client.send(TdApi.SetAuthenticationPhoneNumber(res, null), object : Client.ResultHandler {
                                    override fun onResult(`object`: TdApi.Object?) {
                                        if (`object` is TdApi.Error) {
                                            authorizationWaitPhoneNumberCot = null
                                            cot.resume(ParaboxInitActionResult.Error("错误（${`object`.message}）"))
                                        }
                                    }
                                })
                            }
                        } else {
                            authorizationWaitPhoneNumberCot = null
                            ParaboxInitActionResult.Done
                        }
                    }
                }
            ),
            ParaboxInitAction.TextInputAction(
                key = "code",
                title = "输入验证码",
                errMsg = "",
                description = "请输入从其他已登录当前Telegram账号的客户端接收的验证码",
                label = "验证码",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                defaultValue = null,
                onSkipCheck = {
                    authorizationState.ordinal > AuthorizationState.WaitCode.ordinal
                },
                onResult = { res: String ->
                    Log.i("tdlib", "getInitAction, code=$res, state=$authorizationState")
                    if (res.isBlank()) {
                        ParaboxInitActionResult.Error("验证码不能为空")
                    } else if (!res.matches(Regex("^[0-9]{5}$"))){
                        ParaboxInitActionResult.Error("验证码格式错误")
                    } else {
                        if (authorizationState <= AuthorizationState.WaitCode) {
                            suspendCoroutine<ParaboxInitActionResult> { cot ->
                                authorizationWaitCodeCot = cot
                                client.send(TdApi.CheckAuthenticationCode(res), object : Client.ResultHandler {
                                    override fun onResult(`object`: TdApi.Object?) {
                                        if (`object` is TdApi.Error) {
                                            authorizationWaitCodeCot = null
                                            cot.resume(ParaboxInitActionResult.Error("错误（${`object`.message}）"))
                                        }
                                    }
                                })
                            }
                        } else {
                            authorizationWaitCodeCot = null
                            ParaboxInitActionResult.Done
                        }
                    }
                }
            ),
            ParaboxInitAction.TextInputAction(
                key = "two_way_password",
                title = "两步验证",
                errMsg = "",
                description = "请输入两步验证密钥",
                label = "密码",
                type = ParaboxInitAction.KeyboardType.TEXT,
                defaultValue = null,
                onSkipCheck = {
                    authorizationState != AuthorizationState.WaitPassword
                },
                onResult = { res: String ->
                    Log.i("tdlib", "getInitAction, two_way_password=$res, state=$authorizationState")
                    if (res.isBlank()) {
                        ParaboxInitActionResult.Error("两步验证码不能为空")
                    } else {
                        if (authorizationState == AuthorizationState.WaitPassword) {
                            suspendCoroutine<ParaboxInitActionResult> { cot ->
                                authorizationWaitPasswordCot = cot
                                client.send(TdApi.CheckAuthenticationPassword(res), object : Client.ResultHandler {
                                    override fun onResult(`object`: TdApi.Object?) {
                                        if (`object` is TdApi.Error) {
                                            authorizationWaitPasswordCot = null
                                            cot.resume(ParaboxInitActionResult.Error("错误（${`object`.message}）"))
                                        }
                                    }
                                })
                            }
                        } else {
                            authorizationWaitPasswordCot = null
                            ParaboxInitActionResult.Done
                        }
                    }
                }
            )
        )
    }

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        return listOf(
            ParaboxConfigItem.Category(
                key = "category_user",
                title = "用户",
                description = "用户配置"
            ),
            ParaboxConfigItem.ActionConfigItem(
                key = "logout",
                title = "登出",
                description = "登出当前账号",
                confirmModel = ParaboxConfigItem.ActionConfigItem.ConfirmModel(
                    title = "登出",
                    description = "确定要登出当前账号？",
                    confirmText = "确定",
                    cancelText = "取消"
                ),
                action = {
                    client.send(TdApi.LogOut(), this)
                    ParaboxInitActionResult.Done
                }
            )
        )
    }

    override fun onDestroy() {
        client.send(TdApi.Close(), this)
    }

    override fun onResult(`object`: TdApi.Object?) {
        Log.d("tdlib", `object`?.toString()?: "")
        when (`object`) {
            is TdApi.UpdateAuthorizationState -> {
                when (`object`.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        authorizationState = AuthorizationState.WaitTdlibParameter
                        setTdLibParameters()
                        client.send(TdApi.SetDatabaseEncryptionKey(), this)
                    }
                    is TdApi.AuthorizationStateWaitPhoneNumber -> {
                        authorizationState = AuthorizationState.WaitPhoneNumber
                        authorizationWaitTdLibCot?.resume(ParaboxInitActionResult.Done)
                        authorizationWaitTdLibCot = null
                    }
                    is TdApi.AuthorizationStateWaitCode -> {
                        authorizationState = AuthorizationState.WaitCode
                        authorizationWaitPhoneNumberCot?.resume(ParaboxInitActionResult.Done)
                        authorizationWaitPhoneNumberCot = null
                    }
                    is TdApi.AuthorizationStateWaitPassword -> {
                        authorizationWaitCodeCot?.resume(ParaboxInitActionResult.Done)
                        authorizationWaitCodeCot = null
                        authorizationState = AuthorizationState.WaitPassword
                    }
                    is TdApi.AuthorizationStateReady -> {
                        authorizationWaitCodeCot?.resume(ParaboxInitActionResult.Done)
                        authorizationWaitCodeCot = null
                        authorizationWaitPasswordCot?.resume(ParaboxInitActionResult.Done)
                        authorizationWaitPasswordCot = null
                        authorizationState = AuthorizationState.Ready
                    }
                    is TdApi.AuthorizationStateClosing, is TdApi.AuthorizationStateClosed -> {
                        authorizationState = AuthorizationState.Closed
                    }
                }
            }
        }
    }

    override fun onException(e: Throwable?) {
        Log.e("tdlib", e?.message ?: "")
    }

    private fun setTdLibParameters() {
        client.send(
            TdApi.SetTdlibParameters(
                false,
                context!!.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_EXT_PREFIX + "td")?.absolutePath,
                context!!.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_EXT_PREFIX + "td")?.absolutePath,
                byteArrayOf(),
                true,
                true,
                true,
                false,
                context!!.resources.getInteger(R.integer.telegram_api_id),
                context!!.resources.getString(R.string.telegram_api_hash),
                Locale.getDefault().language,
                Build.MODEL,
                Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME
            ), this
        )
    }
}