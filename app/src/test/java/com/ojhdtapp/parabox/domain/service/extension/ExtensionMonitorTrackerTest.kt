package com.ojhdtapp.parabox.domain.service.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionMonitorTrackerTest {

    private fun snap(
        id: Long,
        alias: String = "conn-$id",
        extName: String = "Ext-$id",
        state: ExtensionConnectionState,
    ) = ExtensionConnectionSnapshot(
        connectionId = id,
        alias = alias,
        extensionName = extName,
        state = state,
    )

    @Test
    fun emptyListProducesZeroCountsAndNoAlerts() {
        val tracker = ExtensionMonitorTracker()
        val update = tracker.onSnapshotsUpdated(emptyList())

        assertEquals(0, update.summary.totalCount)
        assertEquals(0, update.summary.activeCount)
        assertEquals(0, update.summary.connectingCount)
        assertEquals(0, update.summary.reconnectingCount)
        assertEquals(0, update.summary.errorCount)
        assertTrue(update.disconnectAlerts.isEmpty())
        assertTrue(update.clearedAlertConnectionIds.isEmpty())
    }

    @Test
    fun mixedStatesComputeAccurateCounts() {
        val tracker = ExtensionMonitorTracker()
        val update = tracker.onSnapshotsUpdated(
            listOf(
                snap(1, state = ExtensionConnectionState.Active),
                snap(2, state = ExtensionConnectionState.Connecting),
                snap(3, state = ExtensionConnectionState.Reconnecting(1, "等待网络恢复")),
                snap(4, state = ExtensionConnectionState.Error("登录态失效")),
            )
        )

        assertEquals(4, update.summary.totalCount)
        assertEquals(1, update.summary.activeCount)
        assertEquals(1, update.summary.connectingCount)
        assertEquals(1, update.summary.reconnectingCount)
        assertEquals(1, update.summary.errorCount)
        assertEquals(2, update.disconnectAlerts.size)
        assertEquals(listOf(3L, 4L), update.disconnectAlerts.map { it.connectionId })
    }

    @Test
    fun activeToReconnectingTriggersSingleAlertAndPreservesRealReasonAcrossRetries() {
        val tracker = ExtensionMonitorTracker()

        val initial = tracker.onSnapshotsUpdated(
            listOf(snap(1, alias = "MyTG", extName = "Telegram", state = ExtensionConnectionState.Active))
        )
        assertTrue(initial.disconnectAlerts.isEmpty())

        val disconnected = tracker.onSnapshotsUpdated(
            listOf(
                snap(
                    1,
                    alias = "MyTG",
                    extName = "Telegram",
                    state = ExtensionConnectionState.Reconnecting(1, "扩展心跳超时，触发重连"),
                )
            )
        )
        assertEquals(1, disconnected.disconnectAlerts.size)
        assertEquals("扩展心跳超时，触发重连", disconnected.disconnectAlerts.first().reason)
        assertEquals("MyTG", disconnected.disconnectAlerts.first().alias)

        // Backoff loop overwrites message with generic text; should not spam alert and should preserve real reason
        val retryGeneric = tracker.onSnapshotsUpdated(
            listOf(
                snap(
                    1,
                    alias = "MyTG",
                    extName = "Telegram",
                    state = ExtensionConnectionState.Reconnecting(1, "正在重新连接扩展服务"),
                )
            )
        )
        assertTrue(retryGeneric.disconnectAlerts.isEmpty())
        val stateAfterGeneric = retryGeneric.summary.items.single().state as ExtensionConnectionState.Reconnecting
        assertEquals("扩展心跳超时，触发重连", stateAfterGeneric.reason)

        // Subsequent retry failure should still not spam
        val retry2 = tracker.onSnapshotsUpdated(
            listOf(
                snap(
                    1,
                    alias = "MyTG",
                    extName = "Telegram",
                    state = ExtensionConnectionState.Reconnecting(2, "UNAVAILABLE: Binder died"),
                )
            )
        )
        assertTrue(retry2.disconnectAlerts.isEmpty())
        val stateAfterRetry2 = retry2.summary.items.single().state as ExtensionConnectionState.Reconnecting
        assertEquals("UNAVAILABLE: Binder died", stateAfterRetry2.reason)
    }

    @Test
    fun recoveryClearsAlertAndSubsequentDisconnectAlertsAgain() {
        val tracker = ExtensionMonitorTracker()

        tracker.onSnapshotsUpdated(listOf(snap(1, state = ExtensionConnectionState.Active)))
        val firstDrop = tracker.onSnapshotsUpdated(
            listOf(snap(1, state = ExtensionConnectionState.Reconnecting(1, "会话流已断开，准备重连")))
        )
        assertEquals(1, firstDrop.disconnectAlerts.size)

        val recovered = tracker.onSnapshotsUpdated(listOf(snap(1, state = ExtensionConnectionState.Active)))
        assertTrue(recovered.disconnectAlerts.isEmpty())
        assertEquals(listOf(1L), recovered.clearedAlertConnectionIds)

        val secondDrop = tracker.onSnapshotsUpdated(
            listOf(snap(1, state = ExtensionConnectionState.Error("Telegram 账号已登出")))
        )
        assertEquals(1, secondDrop.disconnectAlerts.size)
        assertEquals("Telegram 账号已登出", secondDrop.disconnectAlerts.first().reason)
    }

    @Test
    fun reconnectingEscalatingToTerminalErrorUpdatesAlertWithNewReason() {
        val tracker = ExtensionMonitorTracker()

        tracker.onSnapshotsUpdated(listOf(snap(1, state = ExtensionConnectionState.Active)))
        tracker.onSnapshotsUpdated(
            listOf(snap(1, state = ExtensionConnectionState.Reconnecting(1, "等待网络恢复")))
        )

        val escalated = tracker.onSnapshotsUpdated(
            listOf(snap(1, state = ExtensionConnectionState.Error("扩展签名授权已撤销")))
        )
        assertEquals(1, escalated.disconnectAlerts.size)
        assertEquals("扩展签名授权已撤销", escalated.disconnectAlerts.first().reason)
    }

    @Test
    fun deletingDisconnectedConnectionClearsStaleAlertWithoutNewAlert() {
        val tracker = ExtensionMonitorTracker()

        tracker.onSnapshotsUpdated(listOf(snap(1, state = ExtensionConnectionState.Error("扩展包未安装"))))
        val afterDelete = tracker.onSnapshotsUpdated(emptyList())

        assertTrue(afterDelete.disconnectAlerts.isEmpty())
        assertEquals(listOf(1L), afterDelete.clearedAlertConnectionIds)
    }

    @Test
    fun multipleConnectionsTrackIndependently() {
        val tracker = ExtensionMonitorTracker()

        tracker.onSnapshotsUpdated(
            listOf(
                snap(1, state = ExtensionConnectionState.Active),
                snap(2, state = ExtensionConnectionState.Active),
            )
        )

        val bothDrop = tracker.onSnapshotsUpdated(
            listOf(
                snap(1, state = ExtensionConnectionState.Reconnecting(1, "网络中断")),
                snap(2, state = ExtensionConnectionState.Error("认证失败")),
            )
        )
        assertEquals(listOf(1L, 2L), bothDrop.disconnectAlerts.map { it.connectionId })

        val oneRecovers = tracker.onSnapshotsUpdated(
            listOf(
                snap(1, state = ExtensionConnectionState.Active),
                snap(2, state = ExtensionConnectionState.Error("认证失败")),
            )
        )
        assertTrue(oneRecovers.disconnectAlerts.isEmpty())
        assertEquals(listOf(1L), oneRecovers.clearedAlertConnectionIds)
        assertEquals(1, oneRecovers.summary.activeCount)
        assertEquals(1, oneRecovers.summary.errorCount)
    }
}
