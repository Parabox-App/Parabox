package com.ojhdt.parabox.extension.tdlib.core

import android.os.Build
import android.util.Log
import com.ojhdt.parabox.extension.tdlib.BuildConfig
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdErrorException(
    val code: Int,
    override val message: String,
) : Exception("TDLib error ($code): $message")

interface TdClientApi {
    val accountId: String
    val updates: Flow<TdApi.Object>
    val authorizationState: StateFlow<TdApi.AuthorizationState?>
    val connectionState: StateFlow<TdApi.ConnectionState?>

    suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object
    suspend fun applyProxy(proxyConfig: TdProxyConfig)
    suspend fun setNetworkType(networkType: TdApi.NetworkType) {
        runCatching { sendRaw(TdApi.SetNetworkType(networkType)) }
    }
    suspend fun closeAndWait(timeoutMs: Long = 5_000L)
}

suspend inline fun <reified T : TdApi.Object> TdClientApi.send(function: TdApi.Function<*>): T {
    val result = sendRaw(function)
    if (result is TdApi.Error) {
        throw TdErrorException(result.code, result.message)
    }
    return result as? T
        ?: throw TdErrorException(-1, "Unexpected TDLib response type: ${result.javaClass.simpleName}")
}

interface TdClientFactory {
    fun create(
        accountId: String,
        directories: TdAccountDirectories,
        apiId: Int,
        apiHash: String,
        proxyConfig: TdProxyConfig,
    ): TdClientApi
}

class RealTdClientFactory : TdClientFactory {
    companion object {
        private val logLevelInitialized = AtomicBoolean(false)
    }

    override fun create(
        accountId: String,
        directories: TdAccountDirectories,
        apiId: Int,
        apiHash: String,
        proxyConfig: TdProxyConfig,
    ): TdClientApi {
        if (logLevelInitialized.compareAndSet(false, true)) {
            runCatching {
                Client.execute(TdApi.SetLogVerbosityLevel(1))
            }
        }
        return RealTdClientWrapper(
            accountId = accountId,
            directories = directories,
            apiId = apiId,
            apiHash = apiHash,
            initialProxy = proxyConfig,
        )
    }
}

class RealTdClientWrapper(
    override val accountId: String,
    private val directories: TdAccountDirectories,
    private val apiId: Int,
    private val apiHash: String,
    private val initialProxy: TdProxyConfig,
) : TdClientApi, Client.ResultHandler, Client.ExceptionHandler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Unbounded channel guarantees zero update loss before TdSession subscribes (startup get_difference)
    // and during large update bursts, without ever blocking TDLib's native callback thread.
    private val updateChannel = Channel<TdApi.Object>(Channel.UNLIMITED)
    override val updates: Flow<TdApi.Object> = updateChannel.receiveAsFlow()

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    override val authorizationState: StateFlow<TdApi.AuthorizationState?> = _authorizationState.asStateFlow()

    private val _connectionState = MutableStateFlow<TdApi.ConnectionState?>(null)
    override val connectionState: StateFlow<TdApi.ConnectionState?> = _connectionState.asStateFlow()

    private val closed = AtomicBoolean(false)
    private val tdlibParamsSent = AtomicBoolean(false)
    private val proxyApplied = AtomicBoolean(false)

    private val client: Client = Client.create(this, this, this)

    init {
        client.send(TdApi.GetAuthorizationState(), this)
    }

    override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object {
        if (closed.get()) {
            throw TdErrorException(-2, "TDLib client is closed")
        }
        return suspendCancellableCoroutine { cont: CancellableContinuation<TdApi.Object> ->
            client.send(
                function,
                { obj ->
                    if (cont.isActive) {
                        if (obj == null) {
                            cont.resumeWithException(TdErrorException(-1, "Null TDLib response"))
                        } else if (obj is TdApi.Error) {
                            cont.resumeWithException(TdErrorException(obj.code, obj.message))
                        } else {
                            cont.resume(obj)
                        }
                    }
                },
                { ex ->
                    if (cont.isActive) {
                        cont.resumeWithException(ex ?: TdErrorException(-1, "Unknown TDLib exception"))
                    }
                },
            )
        }
    }

    override suspend fun applyProxy(proxyConfig: TdProxyConfig) {
        if (closed.get()) return
        runCatching {
            val existing = send<TdApi.Proxies>(TdApi.GetProxies())
            if (!proxyConfig.enabled || !proxyConfig.isValid()) {
                send<TdApi.Ok>(TdApi.DisableProxy())
                existing.proxies.forEach { proxy ->
                    runCatching { send<TdApi.Ok>(TdApi.RemoveProxy(proxy.id)) }
                }
            } else {
                existing.proxies.forEach { proxy ->
                    runCatching { send<TdApi.Ok>(TdApi.RemoveProxy(proxy.id)) }
                }
                send<TdApi.Proxy>(
                    TdApi.AddProxy(
                        proxyConfig.host,
                        proxyConfig.port,
                        true,
                        proxyConfig.toTdApiProxyType(),
                    )
                )
            }
        }.onFailure { err ->
            Log.w(TAG, "[$accountId] Failed to apply proxy: ${err.message}")
        }
    }

    override suspend fun setNetworkType(networkType: TdApi.NetworkType) {
        if (closed.get()) return
        Log.i(TAG, "[$accountId] Applying TdApi.SetNetworkType(${networkType.javaClass.simpleName})")
        runCatching {
            send<TdApi.Ok>(TdApi.SetNetworkType(networkType))
        }.onFailure { err ->
            Log.w(TAG, "[$accountId] Failed to set network type ${networkType.javaClass.simpleName}: ${err.message}")
        }
    }

    override suspend fun closeAndWait(timeoutMs: Long) {
        if (!closed.compareAndSet(false, true)) return
        if (_authorizationState.value !is TdApi.AuthorizationStateClosed) {
            runCatching {
                client.send(TdApi.Close(), null)
            }
            withTimeoutOrNull(timeoutMs) {
                _authorizationState.first { it is TdApi.AuthorizationStateClosed }
            }
        }
        updateChannel.close()
        scope.cancel()
    }

    override fun onResult(`object`: TdApi.Object?) {
        if (`object` == null) return
        when (`object`) {
            is TdApi.UpdateAuthorizationState -> {
                val state = `object`.authorizationState
                _authorizationState.value = state
                handleAuthorizationBootstrap(state)
            }
            is TdApi.AuthorizationState -> {
                _authorizationState.value = `object`
                handleAuthorizationBootstrap(`object`)
            }
            is TdApi.UpdateConnectionState -> {
                _connectionState.value = `object`.state
            }
        }
        updateChannel.trySend(`object`)
    }

    override fun onException(e: Throwable?) {
        Log.e(TAG, "[$accountId] TDLib handler exception", e)
    }

    private fun handleAuthorizationBootstrap(state: TdApi.AuthorizationState) {
        if (state is TdApi.AuthorizationStateWaitTdlibParameters && tdlibParamsSent.compareAndSet(false, true)) {
            val params = TdApi.SetTdlibParameters(
                false,
                directories.dbDir.absolutePath,
                directories.filesDir.absolutePath,
                byteArrayOf(),
                true,
                true,
                true,
                false,
                apiId,
                apiHash,
                Locale.getDefault().language.ifBlank { "en" },
                Build.MODEL ?: "Android",
                Build.VERSION.RELEASE ?: "14",
                BuildConfig.VERSION_NAME,
            )
            client.send(params) { obj ->
                if (obj is TdApi.Error) {
                    Log.e(TAG, "[$accountId] SetTdlibParameters error: ${obj.code} ${obj.message}")
                } else if (proxyApplied.compareAndSet(false, true)) {
                    scope.launch { applyProxy(initialProxy) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "RealTdClientWrapper"
    }
}
