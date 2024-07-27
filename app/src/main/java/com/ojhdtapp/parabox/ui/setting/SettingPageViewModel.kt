package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.repository.ConnectionInfoRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val extensionManager: ExtensionManager,
    val connectionInfoRepository: ConnectionInfoRepository,
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

            is SettingPageEvent.UpdateConnection -> {
                state.copy(
                    connectionList = event.list
                )
            }

            is SettingPageEvent.UpdateExtension -> {
                state.copy(
                    extensionList = event.list
                )
            }

            is SettingPageEvent.DeleteConnection -> {
                viewModelScope.launch(Dispatchers.IO) {
                    connectionInfoRepository.deleteConnectionInfoById(event.extensionId)
                }
                state
            }

            is SettingPageEvent.UpdateExtensionInitActionState -> {
                state.copy(
                    initActionState = event.state
                )
            }

            is SettingPageEvent.InitNewConnection -> {
                initNewExtensionConnection(event.extension)
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
                // don't clear the UI state
                state
            }

            is SettingPageEvent.RestartExtensionConnection -> {
                extensionManager.restartConnection(event.extensionId)
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

            is SettingPageEvent.ReloadExtension -> {
                extensionManager.reloadExtension()
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
                    sendEvent(SettingPageEvent.TagLabelChatsLoadDone(it.data ?: emptyList(), LoadState.SUCCESS))
                } else {
                    sendEvent(SettingPageEvent.TagLabelChatsLoadDone(emptyList(), LoadState.ERROR))
                }
            }
        }
    }

    private var initActionStateCollectionJob: Job? = null
    private fun initNewExtensionConnection(extension: Extension.Success) {
        initActionStateCollectionJob?.cancel()
        initActionStateCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            extensionManager.initActionStateFlow.collectLatest {
                if (it != null) {
                    sendEvent(
                        SettingPageEvent.UpdateExtensionInitActionState(
                            SettingPageState.InitActionState(
                                name = it.name,
                                actionList = it.actionList,
                                currentIndex = it.currentIndex
                            )
                        )
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.initNewExtensionConnection(extension)
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
        viewModelScope.launch {
            extensionManager.resetInitAction(isDone)
        }
    }

    init {
        viewModelScope.launch {
            getChat.notificationDisabled().distinctUntilChangedBy { it.data?.size }.collectLatest {
                if (it is Resource.Success) {
                    sendEvent(
                        SettingPageEvent.NotificationDisabledChatLoadDone(
                            it.data ?: emptyList(),
                            LoadState.SUCCESS
                        )
                    )
                } else {
                    sendEvent(SettingPageEvent.NotificationDisabledChatLoadDone(emptyList(), LoadState.ERROR))
                }
            }
        }
        viewModelScope.launch {
            extensionManager.extensionFlow.collectLatest {
                sendEvent(SettingPageEvent.UpdateExtension(it))
            }
        }
        viewModelScope.launch {
            extensionManager.connectionFlow.collectLatest {
                sendEvent(SettingPageEvent.UpdateConnection(it))
            }
        }
    }
}