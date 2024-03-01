package com.ojhdtapp.parabox.ui.setting

import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface SettingPageEvent : UiEvent{
    data class SelectSetting(val setting: Setting) : SettingPageEvent
}