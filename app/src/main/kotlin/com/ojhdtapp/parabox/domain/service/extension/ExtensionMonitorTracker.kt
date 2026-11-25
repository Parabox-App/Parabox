package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus

sealed interface ExtensionConnectionState {
    data object Connecting : ExtensionConnectionState
    data object Active : ExtensionConnectionState
    data class Reconnecting(val attempt: Int, val reason: String) : ExtensionConnectionState
    data class Error(val reason: String) : ExtensionConnectionState
}

data class ExtensionConnectionSnapshot(
    val connectionId: Long,
    val alias: String,
    val extensionName: String,
    val state: ExtensionConnectionState,
) {
    companion object {
        fun fromPending(connection: Connection.ConnectionPending): ExtensionConnectionSnapshot =
            ExtensionConnectionSnapshot(
                connectionId = connection.connectionId,
                alias = connection.alias,
                extensionName = connection.name,
                state = ExtensionConnectionState.Connecting,
            )

        fun fromFail(connection: Connection.ConnectionFail): ExtensionConnectionSnapshot =
            ExtensionConnectionSnapshot(
                connectionId = connection.connectionId,
                alias = connection.alias,
                extensionName = connection.name,
                state = ExtensionConnectionState.Error(connection.reason.trim()),
            )

        fun fromSuccess(
            connection: Connection.ConnectionSuccess,
            status: ParaboxConnectionStatus,
        ): ExtensionConnectionSnapshot {
            val state = when (status) {
                is ParaboxConnectionStatus.Pending,
                is ParaboxConnectionStatus.Initializing,
                -> ExtensionConnectionState.Connecting

                is ParaboxConnectionStatus.Active -> ExtensionConnectionState.Active

                is ParaboxConnectionStatus.Reconnecting -> ExtensionConnectionState.Reconnecting(
                    attempt = status.attempt.coerceAtLeast(1),
                    reason = status.message.trim(),
                )

                is ParaboxConnectionStatus.Error -> ExtensionConnectionState.Error(
                    reason = status.message.trim(),
                )
            }
            return ExtensionConnectionSnapshot(
                connectionId = connection.connectionId,
                alias = connection.alias,
                extensionName = connection.name,
                state = state,
            )
        }
    }
}

data class ExtensionMonitorSummary(
    val totalCount: Int,
    val activeCount: Int,
    val connectingCount: Int,
    val reconnectingCount: Int,
    val errorCount: Int,
    val items: List<ExtensionConnectionSnapshot>,
)

data class DisconnectAlert(
    val connectionId: Long,
    val alias: String,
    val extensionName: String,
    val reason: String,
)

data class ExtensionMonitorUpdate(
    val summary: ExtensionMonitorSummary,
    val disconnectAlerts: List<DisconnectAlert>,
    val clearedAlertConnectionIds: List<Long>,
)

/**
 * Pure state-machine tracker for extension connection health.
 * Computes live summary counts and deduplicated disconnect/recovery alert events.
 */
class ExtensionMonitorTracker {

    private data class TrackedConnection(
        val state: ExtensionConnectionState,
        val alertActive: Boolean,
        val lastAlertReason: String?,
        val lastMeaningfulReason: String?,
    )

    private val tracked = mutableMapOf<Long, TrackedConnection>()

    fun onSnapshotsUpdated(snapshots: List<ExtensionConnectionSnapshot>): ExtensionMonitorUpdate {
        val disconnectAlerts = mutableListOf<DisconnectAlert>()
        val clearedAlertIds = mutableListOf<Long>()
        val normalizedSnapshots = ArrayList<ExtensionConnectionSnapshot>(snapshots.size)

        val currentIds = HashSet<Long>(snapshots.size)
        for (snapshot in snapshots) {
            currentIds.add(snapshot.connectionId)
            val prev = tracked[snapshot.connectionId]
            val normalizedState = when (val state = snapshot.state) {
                is ExtensionConnectionState.Active -> {
                    if (prev?.alertActive == true) {
                        clearedAlertIds.add(snapshot.connectionId)
                    }
                    tracked[snapshot.connectionId] = TrackedConnection(
                        state = state,
                        alertActive = false,
                        lastAlertReason = null,
                        lastMeaningfulReason = null,
                    )
                    state
                }

                is ExtensionConnectionState.Connecting -> {
                    if (prev?.alertActive == true) {
                        clearedAlertIds.add(snapshot.connectionId)
                    }
                    tracked[snapshot.connectionId] = TrackedConnection(
                        state = state,
                        alertActive = false,
                        lastAlertReason = null,
                        lastMeaningfulReason = null,
                    )
                    state
                }

                is ExtensionConnectionState.Reconnecting -> {
                    val meaningfulReason = if (!isGenericReconnectReason(state.reason)) {
                        state.reason
                    } else {
                        prev?.lastMeaningfulReason
                    }
                    val effectiveReason = meaningfulReason ?: state.reason
                    val effectiveState = state.copy(reason = effectiveReason)
                    val shouldAlert = prev == null || !prev.alertActive
                    if (shouldAlert) {
                        disconnectAlerts.add(
                            DisconnectAlert(
                                connectionId = snapshot.connectionId,
                                alias = snapshot.alias,
                                extensionName = snapshot.extensionName,
                                reason = effectiveReason,
                            )
                        )
                    }
                    tracked[snapshot.connectionId] = TrackedConnection(
                        state = effectiveState,
                        alertActive = true,
                        lastAlertReason = if (shouldAlert) effectiveReason else prev?.lastAlertReason,
                        lastMeaningfulReason = meaningfulReason,
                    )
                    effectiveState
                }

                is ExtensionConnectionState.Error -> {
                    val effectiveReason = state.reason.ifBlank { prev?.lastMeaningfulReason.orEmpty() }
                    val effectiveState = state.copy(reason = effectiveReason)
                    val shouldAlert = when {
                        prev == null || !prev.alertActive -> true
                        effectiveReason.isNotBlank() && effectiveReason != prev.lastAlertReason -> true
                        else -> false
                    }
                    if (shouldAlert) {
                        disconnectAlerts.add(
                            DisconnectAlert(
                                connectionId = snapshot.connectionId,
                                alias = snapshot.alias,
                                extensionName = snapshot.extensionName,
                                reason = effectiveReason,
                            )
                        )
                    }
                    tracked[snapshot.connectionId] = TrackedConnection(
                        state = effectiveState,
                        alertActive = true,
                        lastAlertReason = if (shouldAlert) effectiveReason else prev?.lastAlertReason,
                        lastMeaningfulReason = effectiveReason.ifBlank { prev?.lastMeaningfulReason },
                    )
                    effectiveState
                }
            }
            normalizedSnapshots.add(snapshot.copy(state = normalizedState))
        }

        // Remove deleted connections and clear any stale disconnect notifications
        val iterator = tracked.entries.iterator()
        while (iterator.hasNext()) {
            val (id, entry) = iterator.next()
            if (id !in currentIds) {
                if (entry.alertActive) {
                    clearedAlertIds.add(id)
                }
                iterator.remove()
            }
        }

        var activeCount = 0
        var connectingCount = 0
        var reconnectingCount = 0
        var errorCount = 0
        for (item in normalizedSnapshots) {
            when (item.state) {
                is ExtensionConnectionState.Active -> activeCount++
                is ExtensionConnectionState.Connecting -> connectingCount++
                is ExtensionConnectionState.Reconnecting -> reconnectingCount++
                is ExtensionConnectionState.Error -> errorCount++
            }
        }

        return ExtensionMonitorUpdate(
            summary = ExtensionMonitorSummary(
                totalCount = normalizedSnapshots.size,
                activeCount = activeCount,
                connectingCount = connectingCount,
                reconnectingCount = reconnectingCount,
                errorCount = errorCount,
                items = normalizedSnapshots,
            ),
            disconnectAlerts = disconnectAlerts,
            clearedAlertConnectionIds = clearedAlertIds,
        )
    }

    private fun isGenericReconnectReason(reason: String): Boolean =
        reason.isBlank() || reason == GENERIC_RECONNECTING_MESSAGE

    companion object {
        private const val GENERIC_RECONNECTING_MESSAGE = "正在重新连接扩展服务"
    }
}
