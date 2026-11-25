package com.ojhdt.parabox.extension.tdlib.core

import org.drinkless.tdlib.TdApi
import org.json.JSONObject

enum class TdProxyProtocol(val label: String) {
    SOCKS5("SOCKS5"),
    HTTP("HTTP"),
    MTPROTO("MTProto");

    companion object {
        val labels: List<String> = entries.map { it.label }

        fun fromString(raw: String?): TdProxyProtocol = when (raw?.trim()?.uppercase()) {
            "HTTP" -> HTTP
            "MTPROTO" -> MTPROTO
            else -> SOCKS5
        }

        fun fromIndex(index: Int): TdProxyProtocol =
            entries.getOrElse(index) { SOCKS5 }
    }
}

data class TdProxyConfig(
    val enabled: Boolean = false,
    val protocol: TdProxyProtocol = TdProxyProtocol.SOCKS5,
    val host: String = "",
    val port: Int = 0,
    val usernameOrSecret: String = "",
    val password: String = "",
) {
    fun isValid(): Boolean {
        if (!enabled) return true
        if (host.isBlank() || port !in 1..65535) return false
        if (protocol == TdProxyProtocol.MTPROTO && usernameOrSecret.isBlank()) return false
        return true
    }

    fun toTdApiProxyType(): TdApi.ProxyType = when (protocol) {
        TdProxyProtocol.SOCKS5 -> TdApi.ProxyTypeSocks5(usernameOrSecret, password)
        TdProxyProtocol.HTTP -> TdApi.ProxyTypeHttp(usernameOrSecret, password, false)
        TdProxyProtocol.MTPROTO -> TdApi.ProxyTypeMtproto(usernameOrSecret)
    }

    companion object {
        const val KEY_ENABLED = "proxy_enabled"
        const val KEY_TYPE = "proxy_type"
        const val KEY_HOST = "proxy_host"
        const val KEY_PORT = "proxy_port"
        const val KEY_USERNAME = "proxy_username"
        const val KEY_PASSWORD = "proxy_password"

        fun fromExtra(extra: JSONObject): TdProxyConfig {
            val enabled = extra.optBoolean(KEY_ENABLED, false)
            val protocol = TdProxyProtocol.fromString(extra.optString(KEY_TYPE))
            val host = extra.optString(KEY_HOST, "").trim()
            val port = extra.optString(KEY_PORT, "").trim().toIntOrNull()
                ?: extra.optInt(KEY_PORT, 0)
            val username = extra.optString(KEY_USERNAME, "").trim()
            val password = extra.optString(KEY_PASSWORD, "")
            return TdProxyConfig(
                enabled = enabled,
                protocol = protocol,
                host = host,
                port = port,
                usernameOrSecret = username,
                password = password,
            )
        }
    }
}
