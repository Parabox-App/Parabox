package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : BaseViewModel<SettingPageState, SettingPageEvent, SettingPageEffect>() {
    override fun initialState(): SettingPageState {
        return SettingPageState()
    }

    override suspend fun handleEvent(event: SettingPageEvent, state: SettingPageState): SettingPageState? {
        return when(event){
            is SettingPageEvent.SelectSetting -> {
                state.copy(
                    selected = event.setting
                )
            }
            else -> state
        }
    }
}