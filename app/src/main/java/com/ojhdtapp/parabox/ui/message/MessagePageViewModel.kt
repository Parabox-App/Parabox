package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.buildFileName
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.GetMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.QuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
        return MessagePageState(
            chatPagingDataFlow = getChat(listOf(ChatFilter.Normal)).cachedIn(viewModelScope),
            pinnedChatPagingDataFlow = getChat.pinned(),
            messagePagingDataFlow = flow { emit(PagingData.empty()) }
        )
    }

    init {
        sendEvent(MessagePageEvent.UpdateDataStore)
    }

    override suspend fun handleEvent(
        event: MessagePageEvent,
        state: MessagePageState
    ): MessagePageState? {
        return when (event) {
            is MessagePageEvent.UpdateDataStore -> {
                state.copy(
                    datastore = state.datastore.copy(
                        enableSwipeToDismiss = context.getDataStoreValue(
                            DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS,
                            true
                        )
                    )
                )
            }

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
                    chatPagingDataFlow = getChat(newList),
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
                    chatPagingDataFlow = getChat(newList),
                    selectedChatFilterLists = newList
                )
            }

            is MessagePageEvent.GetChatPagingDataFlow -> {
                return state.copy(
                    chatPagingDataFlow = getChat(state.selectedChatFilterLists)
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

            is MessagePageEvent.LoadMessage -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        chat = event.chat
                    ),
                    messagePagingDataFlow = getMessage(event.chat.subChatIds).cachedIn(viewModelScope)
                )
            }

            is MessagePageEvent.OpenEditArea -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            expanded = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateEditAreaInput -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            input = event.input,
                            iconShrink = when {
                                event.input.length > 6 -> true
                                event.input.isEmpty() -> false
                                else -> state.currentChat.editAreaState.iconShrink
                            }
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateToolbarState -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            toolbarState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.EnableAudioRecorder -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            enableAudioRecorder = event.enable
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateAudioRecorderState -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            audioRecorderState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerMenu -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        imagePreviewerState = state.currentChat.imagePreviewerState.copy(
                            expandMenu = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerToolbar -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        imagePreviewerState = state.currentChat.imagePreviewerState.copy(
                            showToolbar = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.AddMemeUri -> {
                val extension = fileUtil.getFileNameExtension(event.imageUri)
                val path = fileUtil.createPathOnExternalFilesDir(
                    FileUtil.EXTERNAL_FILES_DIR_MEME,
                    buildFileName(FileUtil.EXTERNAL_FILES_DIR_MEME, extension ?: FileUtil.DEFAULT_IMAGE_EXTENSION)
                )
                if(fileUtil.copyFileToPath(event.imageUri, path)){
                    event.onSuccess(path)
                } else {
                    event.onFailure()
                }
                return state
            }

            is MessagePageEvent.SaveImageToLocal -> {
//                fileUtil.saveImageToExternalStorage(event.image.resourceInfo.getModel())
                return state
            }

            is MessagePageEvent.AddImageUriToChosenList -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            chosenImageList = state.currentChat.editAreaState.chosenImageList.toMutableList().apply {
                                add(event.imageUri)
                            }
                        ),
                    )
                )
            }

            is MessagePageEvent.ShowVoicePermissionDeniedDialog -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            showVoicePermissionDeniedDialog = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateIconShrink -> {
                return state.copy(
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
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
                    currentChat = state.currentChat.copy(
                        editAreaState = state.currentChat.editAreaState.copy(
                            input = "",
                            chosenImageList = emptyList(),
                            chosenAudioUri = null,
                            chosenFileUri = null,
                            chosenAtId = null,
                            chosenQuoteReplyMessageId = null,
                            audioRecorderState = AudioRecorderState.Ready,
                            iconShrink = false
                        )
                    )
                )
            }
        }
    }

    private val chatLatestMessageSenderMap = mutableMapOf<Long, Resource<Contact>>()

    fun getLatestMessageSenderWithCache(senderId: Long?): Flow<Resource<Contact>> {
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

    fun buildParaboxSendMessage(){
        buildList<ParaboxMessageElement> {
            if(uiState.value.currentChat.editAreaState.input.isNotEmpty()){
                add(ParaboxPlainText(uiState.value.currentChat.editAreaState.input))
            }
            uiState.value.currentChat.editAreaState.chosenImageList.forEach {
                add(ParaboxImage(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(it)))
            }
            if(uiState.value.currentChat.editAreaState.chosenAudioUri != null){
                add(ParaboxAudio(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uiState.value.currentChat.editAreaState.chosenAudioUri!!)))
            }
//            if(uiState.value.currentChat.editAreaState.chosenFileUri != null){
//                add(ParaboxFile(uiState.value.currentChat.editAreaState.chosenFileUri!!.toString()))
//            }
        }
    }
}