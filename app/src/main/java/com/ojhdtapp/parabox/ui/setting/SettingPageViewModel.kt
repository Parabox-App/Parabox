package com.ojhdtapp.parabox.ui.setting

import android.content.Context
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
        return SettingPageState(
            labelDetailState = SettingPageState.LabelDetailState()
        )
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