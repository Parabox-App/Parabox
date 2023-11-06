package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.buildFileName
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.GetMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getChat: GetChat,
    val getMessage: GetMessage,
    val getContact: GetContact,
    val updateChat: UpdateChat,
    val fileUtil: FileUtil,
) : BaseViewModel<MessagePageState, MessagePageEvent, MessagePageEffect>() {

    override fun initialState(): MessagePageState {
        return MessagePageState()
    }


    override suspend fun handleEvent(
        event: MessagePageEvent,
        state: MessagePageState
    ): MessagePageState? {
        return when (event) {
            is MessagePageEvent.OpenEnabledChatFilterDialog -> {
                state.copy(
                    openEnabledChatFilterDialog = event.open
                )
            }

            is MessagePageEvent.UpdateEditingChatTags -> {
                state.copy(
                    editingChatTags = event.obj
                )
            }

            is MessagePageEvent.UpdateEnabledChatFilterList -> {
                val newList = state.selectedChatFilterLists.toMutableList()
                    .apply {
                        retainAll(event.list)
                        if (isEmpty()) {
                            add(ChatFilter.Normal)
                        }
                    }
                state.copy(
                    enabledChatFilterList = event.list,
                    selectedChatFilterLists = newList,
                )
            }

            is MessagePageEvent.AddOrRemoveSelectedChatFilter -> {
                if (event.filter is ChatFilter.Normal) return state
                val newList = if (state.selectedChatFilterLists.contains(event.filter)) {
                    state.selectedChatFilterLists.toMutableList().apply {
                        remove(event.filter)
                    }
                } else {
                    state.selectedChatFilterLists.toMutableList().apply {
                        add(event.filter)
                    }
                }.apply {
                    if (isEmpty()) {
                        add(ChatFilter.Normal)
                    } else {
                        remove(ChatFilter.Normal)
                    }
                }
                return state.copy(
                    selectedChatFilterLists = newList
                )
            }

            is MessagePageEvent.UpdateChatUnreadMessagesNum -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.unreadMessagesNum(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    updateChat.unreadMessagesNum(event.chatId, event.oldValue)
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatPin -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.pin(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.pin(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatHide -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.hide(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.hide(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatArchive -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.archive(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.archive(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatTags -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.tags(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.tags(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.ShowChatDetail -> {
                Toast.makeText(context, event.detail.chatId.toString(), Toast.LENGTH_SHORT).show()
                state
            }

            is MessagePageEvent.LoadMessage -> {
                if (event.chat == null) {
                    return state.copy(
                        chatDetail = MessagePageState.ChatDetail(),
                    )
                } else {
                    return state.copy(
                        chatDetail = state.chatDetail.copy(
                            chat = event.chat,
                            editAreaState = state.chatDetail.editAreaState.copy(
                                memeList = refreshMemeList()
                            )
                        )
                    )
                }

            }

            is MessagePageEvent.OpenEditArea -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            expanded = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateEditAreaInput -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            input = event.input,
                            iconShrink = when {
                                event.input.text.length > 6 -> true
                                event.input.text.isEmpty() -> false
                                else -> state.chatDetail.editAreaState.iconShrink
                            }
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateToolbarState -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            toolbarState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.EnableAudioRecorder -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            enableAudioRecorder = event.enable
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateAudioRecorderState -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            audioRecorderState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerMenu -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        imagePreviewerState = state.chatDetail.imagePreviewerState.copy(
                            expandMenu = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerToolbar -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        imagePreviewerState = state.chatDetail.imagePreviewerState.copy(
                            showToolbar = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateImagePreviewerSnapshotList -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        imagePreviewerState = state.chatDetail.imagePreviewerState.copy(
                            imageSnapshotList = event.list,
                            targetElementIndex = event.targetElementIndex
                        )
                    )
                )
            }

            is MessagePageEvent.AddMeme -> {
                val extension = fileUtil.getFileNameExtension(event.meme)
                val path = fileUtil.createPathOnExternalFilesDir(
                    FileUtil.EXTERNAL_FILES_DIR_MEME,
                    buildFileName(FileUtil.EXTERNAL_FILES_DIR_MEME, extension ?: FileUtil.DEFAULT_IMAGE_EXTENSION)
                )
                if (fileUtil.copyFileToPath(event.meme, path)) {
                    event.onSuccess(path)
                } else {
                    event.onFailure()
                }
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            memeList = refreshMemeList()
                        )
                    )
                )
            }

            is MessagePageEvent.RemoveMeme -> {
                val fileName = fileUtil.getFileName(event.meme)
                if (fileName != null && fileUtil.deleteFileOnExternalFilesDir(
                        FileUtil.EXTERNAL_FILES_DIR_MEME,
                        fileName
                    )
                ) {
                    event.onSuccess()
                } else {
                    event.onFailure()
                }
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            memeList = refreshMemeList()
                        )
                    )
                )
            }

            is MessagePageEvent.SaveImageToLocal -> {
//                fileUtil.saveImageToExternalStorage(event.image.resourceInfo.getModel())
                return state
            }

            is MessagePageEvent.AddImageUriToChosenList -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            chosenImageList = state.chatDetail.editAreaState.chosenImageList.toMutableList().apply {
                                add(event.imageUri)
                            }
                        ),
                    )
                )
            }

            is MessagePageEvent.ShowVoicePermissionDeniedDialog -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            showVoicePermissionDeniedDialog = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateIconShrink -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            iconShrink = event.shouldShrink
                        )
                    )
                )
            }

            is MessagePageEvent.SendMessage -> {
                viewModelScope.launch {
                    buildParaboxSendMessage()
                }
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            input = TextFieldValue(""),
                            chosenImageList = emptyList(),
                            chosenAudioUri = null,
                            chosenAtId = null,
                            chosenQuoteReplyMessageId = null,
                            audioRecorderState = AudioRecorderState.Ready,
                            iconShrink = false
                        )
                    )
                )
            }

            is MessagePageEvent.SendMemeMessage -> {
                return state
            }

            is MessagePageEvent.SendFileMessage -> {
                return state
            }

            is MessagePageEvent.AddOrRemoveSelectedMessage -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        selectedMessageList = state.chatDetail.selectedMessageList.toMutableList().apply {
                            if (contains(event.msg)) {
                                remove(event.msg)
                            } else {
                                add(event.msg)
                            }
                        }
                    )
                )
            }

            is MessagePageEvent.ClearSelectedMessage -> {
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        selectedMessageList = emptyList()
                    )
                )
            }
        }
    }

    private val chatLatestMessageSenderMap = mutableMapOf<Long, Resource<Contact>>()

    fun getMessageSenderWithCache(senderId: Long?): Flow<Resource<Contact>> {
        return flow {
            if (senderId == null) {
                emit(Resource.Error("no sender"))
            } else {
                if (chatLatestMessageSenderMap[senderId] != null) {
                    emit(chatLatestMessageSenderMap[senderId]!!)
                } else {
                    emitAll(
                        getContact.byId(senderId).onEach {
                            if (it is Resource.Success) {
                                chatLatestMessageSenderMap[senderId] = it
                            }
                        }
                    )
                }
            }
        }
    }

    fun refreshMemeList(): List<Uri> {
        return FileUtils.listFiles(
            context.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_MEME),
            arrayOf("jpg", "jpeg", "png", "gif"), true
        ).reversed().map {
            it.toUri()
        }
    }

    fun buildParaboxSendMessage() {
        buildList<ParaboxMessageElement> {
            if (uiState.value.chatDetail.editAreaState.input.text.isNotEmpty()) {
                add(ParaboxPlainText(uiState.value.chatDetail.editAreaState.input.text))
            }
            uiState.value.chatDetail.editAreaState.chosenImageList.forEach {
                add(ParaboxImage(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(it)))
            }
            if (uiState.value.chatDetail.editAreaState.chosenAudioUri != null) {
                add(ParaboxAudio(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uiState.value.chatDetail.editAreaState.chosenAudioUri!!)))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagePagingDataFlow: Flow<PagingData<ChatPageUiModel>> =
        uiState.distinctUntilChanged { old, new -> old.chatDetail.chat == new.chatDetail.chat || new.chatDetail.chat == null }
            .flatMapLatest {
                getMessage(buildList {
                    add(it.chatDetail.chat!!.chatId)
                    addAll(it.chatDetail.chat.subChatIds)
                }.distinct(), emptyList())
            }.cachedIn(viewModelScope)
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>> =
        uiState.distinctUntilChangedBy { it.selectedChatFilterLists }
            .flatMapLatest { getChat(it.selectedChatFilterLists) }.cachedIn(viewModelScope)
    val pinnedChatPagingDataFlow: Flow<PagingData<Chat>> =
        getChat.pinned().cachedIn(viewModelScope)
}