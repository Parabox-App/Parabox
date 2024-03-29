package com.ojhdtapp.parabox.ui.setting

import android.content.pm.PackageInfo
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState

data class SettingPageState(
    @Deprecated("using decompose")
    val selected: Setting = Setting.GENERAL,
    val packageInfo: List<PackageInfo> = emptyList(),
    val extension: List<Extension> = emptyList(),
    val labelDetailState: LabelDetailState,
    val notificationState: NotificationState,
) : UiState {
    data class LabelDetailState(
        val selected: ChatFilter.Tag? = null,
        val chatList: List<Chat> = emptyList(),
        val loadState: LoadState = LoadState.LOADING,
    )
    data class NotificationState(
        val chatList: List<Chat> = emptyList(),
        val loadState: LoadState = LoadState.LOADING,
    )
}

enum class Setting {
    GENERAL, ADDONS, LABELS, APPEARANCE, NOTIFICATION, STORAGE, EXPERIMENTAL, HELP
}