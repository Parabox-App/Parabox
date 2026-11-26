package com.ojhdt.parabox.extension.tdlib.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Observes Android default network transitions and maps them to TDLib [TdApi.NetworkType].
 *
 * IMPORTANT: In TDLib, EVERY call to `SetNetworkType` forces all active MTProto sockets to close
 * and reopen — even when the [TdApi.NetworkType] class is unchanged. Android's
 * `onCapabilitiesChanged` fires frequently for RSSI / bandwidth jitter on the exact same network,
 * so we strictly deduplicate by `(networkHandle, networkTypeClass)` and only notify TDLib on
 * genuine network switches or transport transitions.
 */
class TdNetworkMonitor(
    context: Context,
) {
    private val connectivityManager: ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val registered = AtomicBoolean(false)
    private val stateLock = Any()

    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null

    private var lastNetworkHandle: Long? = null
    private var lastNetworkTypeClass: Class<out TdApi.NetworkType>? = null

    fun currentNetworkType(): TdApi.NetworkType {
        val cm = connectivityManager ?: return TdApi.NetworkTypeOther()
        val activeNetwork = cm.activeNetwork ?: return TdApi.NetworkTypeNone()
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return TdApi.NetworkTypeNone()
        return mapCapabilities(capabilities)
    }

    fun start(onNetworkTypeChanged: (TdApi.NetworkType) -> Unit) {
        val cm = connectivityManager ?: return
        if (!registered.compareAndSet(false, true)) return

        // Seed initial state without forcing an immediate reconnect before any client exists
        val initialNetwork = cm.activeNetwork
        val initialType = initialNetwork?.let { cm.getNetworkCapabilities(it) }?.let(::mapCapabilities)
            ?: TdApi.NetworkTypeNone()
        synchronized(stateLock) {
            lastNetworkHandle = initialNetwork?.networkHandle
            lastNetworkTypeClass = initialType.javaClass
        }
        Log.i(TAG, "Initial network state: handle=${initialNetwork?.networkHandle}, type=${initialType.javaClass.simpleName}")
        onNetworkTypeChanged(initialType)

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val type = cm.getNetworkCapabilities(network)?.let(::mapCapabilities)
                    ?: currentNetworkType()
                emitIfChanged(network.networkHandle, type, "onAvailable", onNetworkTypeChanged)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val type = mapCapabilities(networkCapabilities)
                emitIfChanged(network.networkHandle, type, "onCapabilitiesChanged", onNetworkTypeChanged)
            }

            override fun onLost(network: Network) {
                val currentActive = cm.activeNetwork
                // Ignore stale onLost callbacks for a network we already switched away from
                if (currentActive != null && currentActive.networkHandle != network.networkHandle) {
                    val currentType = cm.getNetworkCapabilities(currentActive)?.let(::mapCapabilities)
                        ?: TdApi.NetworkTypeOther()
                    emitIfChanged(currentActive.networkHandle, currentType, "onLost(switched)", onNetworkTypeChanged)
                    return
                }
                val fallbackType = currentActive?.let { cm.getNetworkCapabilities(it) }?.let(::mapCapabilities)
                    ?: TdApi.NetworkTypeNone()
                emitIfChanged(currentActive?.networkHandle, fallbackType, "onLost", onNetworkTypeChanged)
            }
        }
        callback = cb
        runCatching {
            cm.registerDefaultNetworkCallback(cb)
        }.onFailure { err ->
            Log.w(TAG, "Failed to register network callback: ${err.message}")
            registered.set(false)
            callback = null
        }
    }

    fun stop() {
        if (!registered.compareAndSet(true, false)) return
        val cb = callback ?: return
        callback = null
        runCatching {
            connectivityManager?.unregisterNetworkCallback(cb)
        }.onFailure { err ->
            Log.w(TAG, "Failed to unregister network callback: ${err.message}")
        }
    }

    private fun emitIfChanged(
        networkHandle: Long?,
        networkType: TdApi.NetworkType,
        trigger: String,
        onNetworkTypeChanged: (TdApi.NetworkType) -> Unit,
    ) {
        val shouldEmit = synchronized(stateLock) {
            val changed = shouldEmitTransition(
                previousHandle = lastNetworkHandle,
                previousTypeClass = lastNetworkTypeClass,
                newHandle = networkHandle,
                newTypeClass = networkType.javaClass,
            )
            if (changed) {
                lastNetworkHandle = networkHandle
                lastNetworkTypeClass = networkType.javaClass
            }
            changed
        }
        if (shouldEmit) {
            Log.i(TAG, "Network changed ($trigger): handle=$networkHandle, type=${networkType.javaClass.simpleName}")
            onNetworkTypeChanged(networkType)
        }
    }

    private fun mapCapabilities(caps: NetworkCapabilities): TdApi.NetworkType {
        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet && !hasVpn) return TdApi.NetworkTypeNone()
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TdApi.NetworkTypeWiFi()
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
                    TdApi.NetworkTypeMobile()
                } else {
                    TdApi.NetworkTypeMobileRoaming()
                }
            }
            else -> TdApi.NetworkTypeOther()
        }
    }

    companion object {
        private const val TAG = "TdNetworkMonitor"

        internal fun shouldEmitTransition(
            previousHandle: Long?,
            previousTypeClass: Class<out TdApi.NetworkType>?,
            newHandle: Long?,
            newTypeClass: Class<out TdApi.NetworkType>,
        ): Boolean {
            return previousHandle != newHandle || previousTypeClass != newTypeClass
        }
    }
}
