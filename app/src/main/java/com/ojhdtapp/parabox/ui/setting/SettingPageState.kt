package com.ojhdtapp.parabox.ui.setting

import android.os.Bundle
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class SettingPageState(
    @Deprecated("using decompose")
    val selected: Setting = Setting.GENERAL,
    val extensionList: List<Extension> = emptyList(),
    val connectionList: List<Connection> = emptyList(),
    val initActionState: InitActionState = InitActionState(),
    val configState: ConfigState = ConfigState(),
    val labelDetailState: LabelDetailState = LabelDetailState(),
    val notificationState: NotificationState = NotificationState(),
) : UiState {
    data class InitActionState(
        val name: String? = null,
        val actionList: List<ParaboxInitAction> = emptyList(),
        val currentIndex: Int = -1
    )
    data class ConfigState(
        val originalConnection: Connection? = null,
        val cacheExtra: Bundle? = null,
        val configList: List<ParaboxConfigItem> = emptyList(),
        val modified: Boolean = false,
        val loadState: LoadState = LoadState.LOADING,
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