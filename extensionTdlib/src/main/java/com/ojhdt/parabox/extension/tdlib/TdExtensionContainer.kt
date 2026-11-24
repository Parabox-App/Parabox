package com.ojhdt.parabox.extension.tdlib

import android.content.Context
import com.ojhdt.parabox.extension.tdlib.core.RealTdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.mapper.AndroidTdMediaBridge
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge

object TdExtensionContainer {
    @Volatile
    private var coordinatorInstance: TdClientCoordinator? = null

    @Volatile
    private var mediaBridgeInstance: TdMediaBridge? = null

    fun getCoordinator(context: Context): TdClientCoordinator {
        return coordinatorInstance ?: synchronized(this) {
            coordinatorInstance ?: run {
                val appContext = context.applicationContext
                val storage = TdAccountStorage(appContext.filesDir)
                val apiId = appContext.resources.getInteger(R.integer.telegram_api_id)
                val apiHash = appContext.resources.getString(R.string.telegram_api_hash)
                TdClientCoordinator(
                    storage = storage,
                    clientFactory = RealTdClientFactory(),
                    apiId = apiId,
                    apiHash = apiHash,
                ).also { coordinatorInstance = it }
            }
        }
    }

    fun getMediaBridge(context: Context): TdMediaBridge {
        return mediaBridgeInstance ?: synchronized(this) {
            mediaBridgeInstance ?: AndroidTdMediaBridge(context.applicationContext).also {
                mediaBridgeInstance = it
            }
        }
    }
}
