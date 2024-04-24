package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val extensionManager: ExtensionManager,
    val extensionInfoRepository: ExtensionInfoRepository,
    val getChat: GetChat,
    val updateChat: UpdateChat
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

            is SettingPageEvent.UpdateExtensionInitActionState -> {
                state.copy(
                    initActionState = SettingPageState.InitActionState(
                        packageInfo = event.initActionWrapper.packageInfo,
                        actionList = event.initActionWrapper.actionList,
                        currentIndex = event.initActionWrapper.currentIndex
                    )
                )
            }

            is SettingPageEvent.InitNewExtensionConnection -> {
                initNewExtensionConnection(event.packageInfo)
                state
            }

            is SettingPageEvent.SubmitExtensionInitActionResult -> {
                submitExtensionInitActionResult(event.result)
                state
            }

            is SettingPageEvent.RevertExtensionInitAction -> {
                revertExtensionInitAction()
                state
            }

            is SettingPageEvent.InitNewExtensionConnectionDone -> {
                resetExtensionInit(event.isDone)
                state.copy(
                    initActionState = SettingPageState.InitActionState()
                )
            }

            is SettingPageEvent.RestartExtensionConnection -> {
                extensionManager.restartExtension(event.extensionId)
                state
            }
            is SettingPageEvent.UpdateSelectedTagLabel -> {
                getChatWithCustomTag(event.tagLabel)
                state.copy(
                    labelDetailState = SettingPageState.LabelDetailState(
                        selected = event.tagLabel
                    )
                )
            }
            is SettingPageEvent.TagLabelChatsLoadDone -> {
                state.copy(
                    labelDetailState = state.labelDetailState.copy(
                        chatList = event.chats,
                        loadState = event.loadState
                    )
                )
            }

            is SettingPageEvent.UpdateChatTags -> {
                viewModelScope.launch(Dispatchers.IO) {
                    updateChat.tags(event.chatId, event.tags)
                }
                state
            }

            is SettingPageEvent.NotificationDisabledChatLoadDone -> {
                state.copy(
                    notificationState = state.notificationState.copy(
                        chatList = event.chats,
                        loadState = event.loadState
                    )
                )
            }

            is SettingPageEvent.UpdateChatNotificationEnabled -> {
                viewModelScope.launch(Dispatchers.IO) {
                    updateChat.notificationEnabled(event.chatId, event.value)
                }
                state
            }
        }
    }

    private var customTagLabelChatCollectionJob: Job? = null
    fun getChatWithCustomTag(customTag: ChatFilter.Tag) {
        customTagLabelChatCollectionJob?.cancel()
        customTagLabelChatCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            getChat.withCustomTag(customTag).distinctUntilChangedBy { it.data?.size }.collectLatest {
                if (it is Resource.Success) {
                    sendEvent(SettingPageEvent.TagLabelChatsLoadDone(it.data?: emptyList(), LoadState.SUCCESS))
                } else {
                    sendEvent(SettingPageEvent.TagLabelChatsLoadDone(emptyList(), LoadState.ERROR))
                }
            }
        }
    }

    private var initActionStateCollectionJob: Job? = null
    private fun initNewExtensionConnection(packageInfo: PackageInfo) {
        initActionStateCollectionJob?.cancel()
        initActionStateCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            extensionManager.initActionWrapperFlow.collectLatest {
                sendEvent(SettingPageEvent.UpdateExtensionInitActionState(it))
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.initNewExtensionConnection(packageInfo)
        }
    }

    private fun submitExtensionInitActionResult(result: Any) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.submitInitActionResult(result)
        }
    }

    private fun revertExtensionInitAction() {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.revertInitAction()
        }
    }

    private fun resetExtensionInit(isDone: Boolean) {
        initActionStateCollectionJob?.cancel()
        initActionStateCollectionJob = null
        extensionManager.resetInitAction(isDone)
    }

    init {
        viewModelScope.launch {
            getChat.notificationDisabled().distinctUntilChangedBy { it.data?.size }.collectLatest {
                if (it is Resource.Success) {
                    sendEvent(SettingPageEvent.NotificationDisabledChatLoadDone(it.data?: emptyList(), LoadState.SUCCESS))
                } else {
                    sendEvent(SettingPageEvent.NotificationDisabledChatLoadDone(emptyList(), LoadState.ERROR))
                }
            }
        }
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