package com.ojhdtapp.parabox.domain.built_in.td

import android.os.Build
import android.util.Log
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudStatus
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCustomCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo.ParaboxEmptyInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TdConnection : ParaboxConnection(), ParaboxCustomCloudService, Client.ResultHandler, Client.ExceptionHandler {
    private var client: Client? = null

    private var initCot: Continuation<Boolean>? = null
    override suspend fun onInitialize(): Boolean {
        return suspendCoroutine<Boolean> { cot ->
            client = Client.create(this, this, this)
            client!!.send(TdApi.SetLogVerbosityLevel(1), this)
            client!!.send(TdApi.SetDatabaseEncryptionKey(), this)
            initCot = cot
            client!!.send(TdApi.GetAuthorizationState(), this)
        }
    }

    override suspend fun onSendMessage(message: SendMessage) {
        TODO("Not yet implemented")
    }

    private fun handleAuthorizationState(updateAuthorizationState: TdApi.UpdateAuthorizationState) {
        when(updateAuthorizationState.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                setTdLibParameters()
            }
            is TdApi.AuthorizationStateReady -> {
                initCot?.resume(true)
                updateStatus(ParaboxConnectionStatus.Active)
            }
            else -> {
                initCot?.resume(false)
                    updateStatus(ParaboxConnectionStatus.Error("用户登录态失效，请登出后重新添加连接（${updateAuthorizationState.authorizationState}）"))
            }
        }
    }

    private fun handleUserStatus(updateUserStatus: TdApi.UpdateUserStatus) {
        when(updateUserStatus.status) {
            is TdApi.UserStatusOffline -> {
                updateStatus(ParaboxConnectionStatus.Error("用户离线"))
            }
            is TdApi.UserStatusOnline -> {
                updateStatus(ParaboxConnectionStatus.Active)
            }
            else -> {

            }
        }
    }

    private fun handleConnectionState(updateConnectionState: TdApi.UpdateConnectionState) {
        when(updateConnectionState.state) {
            is TdApi.ConnectionStateReady -> {
                updateStatus(ParaboxConnectionStatus.Active)
            }
            else -> {
                updateStatus(ParaboxConnectionStatus.Initializing)
            }
        }
    }

    private fun handleNewMessage(updateNewMessage: TdApi.UpdateNewMessage) {

    }

    private fun handleUser(updateNewUser: TdApi.UpdateUser) {
        val contact = ParaboxContact(
            basicInfo = ParaboxBasicInfo(
                name = updateNewUser.user.firstName + (updateNewUser.user.lastName.takeIf { it.isNotBlank() }?.let { " ${it}" } ?: ""),
                avatar = updateNewUser.user.profilePhoto?.let {
                    ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo(
                        key = "td",
                        id = it.small.remote.id,
                        extra = JSONObject().apply {
                            put("uniqueId", it.small.remote.uniqueId)
                        }.toString()
                    )
                } ?: ParaboxResourceInfo.ParaboxEmptyInfo
            ),
            uid = updateNewUser.user.id.toString()
        )
        coroutineScope.launch {
            receiveContact(contact)
        }
    }

    override fun onResult(`object`: TdApi.Object?) {
        Log.d("TdConnection", "onResult: $`object`")
        when(`object`) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                setTdLibParameters()
            }
            is TdApi.UpdateAuthorizationState -> {
                handleAuthorizationState(`object`)
            }
            is TdApi.UpdateUserStatus -> {
                handleUserStatus(`object`)
            }
            is TdApi.UpdateConnectionState -> {
                handleConnectionState(`object`)
            }
            is TdApi.UpdateNewMessage -> {
                handleNewMessage(`object`)
            }

            is TdApi.UpdateUser -> {
                handleUser(`object`)
            }

            is TdApi.UpdateUnreadChatCount -> {

            }
            is TdApi.UpdateUnreadMessageCount -> {

            }

        }

    }

    override fun onException(e: Throwable?) {
        TODO("Not yet implemented")
    }

    private fun setTdLibParameters() {
        client?.send(
            TdApi.SetTdlibParameters(
                false,
                context.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_EXT_PREFIX + "td")?.absolutePath,
                context.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_EXT_PREFIX + "td")?.absolutePath,
                byteArrayOf(),
                true,
                true,
                true,
                false,
                context.resources.getInteger(R.integer.telegram_api_id),
                context.resources.getString(R.string.telegram_api_hash),
                Locale.getDefault().language,
                Build.MODEL,
                Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME
            ), this
        )
    }

    override suspend fun download(remoteResource: ParaboxResourceInfo.ParaboxRemoteInfo.CustomRemoteInfo): Flow<ParaboxCloudStatus> {
        if (client == null) {
            return MutableStateFlow(ParaboxCloudStatus.Failed)
        } else {
            return flow {
                emit(ParaboxCloudStatus.Waiting(remoteResource))
                client!!.send(TdApi.DownloadFile().apply {
                    fileId = remoteResource.id.toInt()
                }, object : Client.ResultHandler {
                    override fun onResult(`object`: TdApi.Object?) {
                        Log.d("hahaha", "download onResult: $`object`")
                    }
                })
            }
        }
    }
}