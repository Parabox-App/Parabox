package com.ojhdtapp.parabox.ui.setting

import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class SettingPageState(
    @Deprecated("using decompose")
    val selected: Setting = Setting.GENERAL,
    val extensionInfoList: List<ExtensionInfo> = emptyList(),
    val extension: List<Extension> = emptyList(),
    val initActionState: InitActionState = InitActionState(),
    val labelDetailState: LabelDetailState = LabelDetailState(),
    val notificationState: NotificationState = NotificationState(),
) : UiState {
    data class InitActionState(
        val name: String? = null,
        val actionList: List<ParaboxInitAction> = emptyList(),
        val currentIndex: Int = -1,
    )
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
    GENERAL, ADDONS, LABELS, APPEARANCE, NOTIFICATION, STORAGE, CLOUD, EXPERIMENTAL, HELP
}