package com.ojhdtapp.parabox.ui.setting

import android.content.pm.PackageInfo
import androidx.datastore.preferences.core.Preferences
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface SettingPageEvent : UiEvent{
    @Deprecated("using decompose")
    data class SelectSetting(val setting: Setting) : SettingPageEvent
    data class UpdatePackageInfo(val list: List<PackageInfo>): SettingPageEvent
    data class UpdateExtension(val list: List<Extension>): SettingPageEvent
    data class DeleteExtensionInfo(val extensionId: Long): SettingPageEvent
    data class RestartExtensionConnection(val extensionId: Long): SettingPageEvent
}