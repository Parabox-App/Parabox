package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val extensionManager: ExtensionManager,
    val extensionInfoRepository: ExtensionInfoRepository,
) : BaseViewModel<SettingPageState, SettingPageEvent, SettingPageEffect>() {
    override fun initialState(): SettingPageState {
        return SettingPageState()
    }

    override suspend fun handleEvent(event: SettingPageEvent, state: SettingPageState): SettingPageState? {
        return when (event) {
            is SettingPageEvent.SelectSetting -> {
                state.copy(
                    selected = event.setting
                )
            }

            is SettingPageEvent.UpdatePackageInfo -> {
                state.copy(
                    packageInfo = event.list
                )
            }

            is SettingPageEvent.UpdateExtension -> {
                state.copy(
                    extension = event.list
                )
            }

            is SettingPageEvent.DeleteExtensionInfo -> {
                viewModelScope.launch(Dispatchers.IO) {
                    extensionInfoRepository.deleteExtensionInfoById(event.extensionId)
                }
                state
            }
            is SettingPageEvent.RestartExtensionConnection -> {
                extensionManager.restartExtension(event.extensionId)
                state
            }
        }
    }

    init {
        viewModelScope.launch {
            extensionManager.extensionPkgFlow.collectLatest {
                sendEvent(SettingPageEvent.UpdatePackageInfo(it))
            }
        }
        viewModelScope.launch {
            extensionManager.extensionFlow.collectLatest {
                sendEvent(SettingPageEvent.UpdateExtension(it))
            }
        }
    }
}