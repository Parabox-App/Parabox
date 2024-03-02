package com.ojhdtapp.parabox.ui.setting

import com.ojhdtapp.parabox.ui.base.UiState

data class SettingPageState(
    val selected: Setting = Setting.GENERAL
) : UiState {

}

enum class Setting {
    GENERAL, ADDONS, LABELS, APPEARANCE, NOTIFICATION, STORAGE, EXPERIMENTAL, HELP
}