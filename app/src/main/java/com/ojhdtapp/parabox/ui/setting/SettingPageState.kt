package com.ojhdtapp.parabox.ui.setting

import android.content.pm.PackageInfo
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.ui.base.UiState

data class SettingPageState(
    @Deprecated("using decompose")
    val selected: Setting = Setting.GENERAL,
    val packageInfo: List<PackageInfo> = emptyList(),
    val extension: List<Extension> = emptyList()
) : UiState {

}

enum class Setting {
    GENERAL, ADDONS, LABELS, APPEARANCE, NOTIFICATION, STORAGE, EXPERIMENTAL, HELP
}