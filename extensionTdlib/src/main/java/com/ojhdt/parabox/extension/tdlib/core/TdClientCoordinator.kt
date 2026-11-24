package com.ojhdt.parabox.extension.tdlib.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates TDLib Client ownership per [accountId].
 *
 * - Different [accountId]s run fully concurrently with isolated native TDLib clients and DB dirs.
 * - For the SAME [accountId], guarantees strict single-owner access so onboarding wizard clients
 *   always complete `TdApi.Close()` + `AuthorizationStateClosed` before a runtime [TdSession] opens
 *   the same SQLite database directory (eliminating TDLib database lock conflicts).
 */
class TdClientCoordinator(
    val storage: TdAccountStorage,
    private val clientFactory: TdClientFactory,
    private val apiId: Int,
    private val apiHash: String,
) {
    private val accountLocks = ConcurrentHashMap<String, Mutex>()
    private val activeSessionClients = ConcurrentHashMap<String, TdClientApi>()
    private val activeWizardClients = ConcurrentHashMap<String, Pair<String, TdClientApi>>()

    private fun lockFor(accountId: String): Mutex =
        accountLocks.computeIfAbsent(accountId) { Mutex() }

    suspend fun getOrCreateWizardClient(
        wizardSessionId: String,
        accountId: String,
        proxyConfig: TdProxyConfig,
    ): TdClientApi {
        return lockFor(accountId).withLock {
            val existing = activeWizardClients[wizardSessionId]
            if (existing != null && existing.first == accountId) {
                return@withLock existing.second
            }
            // If any stale client holds this accountId, close it first
            activeSessionClients.remove(accountId)?.closeAndWait()
            val dirs = storage.getDirectories(accountId)
            val created = clientFactory.create(accountId, dirs, apiId, apiHash, proxyConfig)
            activeWizardClients[wizardSessionId] = accountId to created
            created
        }
    }

    fun getWizardClient(wizardSessionId: String): TdClientApi? =
        activeWizardClients[wizardSessionId]?.second

    suspend fun closeWizardClient(wizardSessionId: String) {
        val entry = activeWizardClients.remove(wizardSessionId) ?: return
        val (accountId, client) = entry
        lockFor(accountId).withLock {
            client.closeAndWait()
        }
    }

    suspend fun openSessionClient(
        accountId: String,
        proxyConfig: TdProxyConfig,
    ): TdClientApi {
        return lockFor(accountId).withLock {
            // Ensure no leftover wizard client still holds this account's DB
            activeWizardClients.entries
                .filter { it.value.first == accountId }
                .forEach { (wizId, pair) ->
                    if (activeWizardClients.remove(wizId, pair)) {
                        pair.second.closeAndWait()
                    }
                }
            // Replace any stale session client cleanly
            activeSessionClients.remove(accountId)?.closeAndWait()
            val dirs = storage.getDirectories(accountId)
            val created = clientFactory.create(accountId, dirs, apiId, apiHash, proxyConfig)
            activeSessionClients[accountId] = created
            created
        }
    }

    suspend fun closeSessionClient(accountId: String, expectedClient: TdClientApi? = null) {
        lockFor(accountId).withLock {
            val current = activeSessionClients[accountId] ?: return@withLock
            if (expectedClient != null && current !== expectedClient) return@withLock
            activeSessionClients.remove(accountId)
            current.closeAndWait()
        }
    }

    suspend fun logoutAndPurgeAccount(accountId: String) {
        lockFor(accountId).withLock {
            activeWizardClients.entries
                .filter { it.value.first == accountId }
                .forEach { (wizId, pair) ->
                    if (activeWizardClients.remove(wizId, pair)) {
                        runCatching { pair.second.sendRaw(TdApi.LogOut()) }
                        pair.second.closeAndWait()
                    }
                }
            val sessionClient = activeSessionClients.remove(accountId)
            if (sessionClient != null) {
                runCatching { sessionClient.sendRaw(TdApi.LogOut()) }
                sessionClient.closeAndWait()
            } else {
                // Temporarily open client to gracefully notify Telegram server of LogOut if DB exists
                val dirs = storage.getDirectories(accountId)
                if (dirs.dbDir.listFiles()?.isNotEmpty() == true) {
                    val temp = clientFactory.create(accountId, dirs, apiId, apiHash, TdProxyConfig())
                    runCatching { temp.sendRaw(TdApi.LogOut()) }
                    temp.closeAndWait(timeoutMs = 3_000L)
                }
            }
            storage.deleteAccount(accountId)
        }
    }

    fun isAccountActive(accountId: String): Boolean = activeSessionClients.containsKey(accountId)

    suspend fun shutdownAll() {
        val wizards = activeWizardClients.keys.toList()
        wizards.forEach { closeWizardClient(it) }
        val sessions = activeSessionClients.keys.toList()
        sessions.forEach { closeSessionClient(it) }
    }
}
