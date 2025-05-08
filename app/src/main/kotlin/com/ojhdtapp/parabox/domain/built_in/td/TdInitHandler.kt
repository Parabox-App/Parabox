package com.ojhdtapp.parabox.domain.built_in.td

import android.util.Log
import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdInitHandler: ParaboxInitHandler(), Client.ResultHandler, Client.ExceptionHandler {
    private val client: Client by lazy {
        Client.create(this, this, this)
    }

    override fun onInit() {
        client.send(TdApi.SetLogVerbosityLevel(1), this)
        client.send(TdApi.SetDatabaseEncryptionKey(), this)
        client.send(TdApi.SetTdlibParameters(
            false,
            context!!.
            ), this)
    }

    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        return listOf(
            ParaboxInitAction.TextInputAction(
                key = "phone_number",
                title = "输入手机号",
                errMsg = "",
                description = "请输入手机号",
                label = "手机号",
                type = ParaboxInitAction.KeyboardType.NUMBER,
                onResult = { res: String ->
                    ParaboxInitActionResult.Done
                }
            )
        )
    }

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun onResult(`object`: TdApi.Object?) {
        TODO("Not yet implemented")
    }

    override fun onException(e: Throwable?) {
        Log.e("tdlib", e?.message ?:"")
    }
}