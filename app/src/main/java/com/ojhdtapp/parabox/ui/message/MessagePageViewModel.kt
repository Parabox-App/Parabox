package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.gms.maps.model.LatLng
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.LocationUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.buildFileName
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
import com.ojhdtapp.parabox.ui.message.chat.EditAreaMode
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.apache.commons.io.FileUtils
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getChat: GetChat,
    val getMessage: GetMessage,
    val getContact: GetContact,
    val updateChat: UpdateChat,
    val locationUtil: LocationUtil,
    val fileUtil: FileUtil
) : BaseViewModel<MessagePageState, MessagePageEvent, MessagePageEffect>() {

    override fun initialState(): MessagePageState {
        return MessagePageState()
    }


    @OptIn(ExperimentalFoundationApi::class)
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

            is MessagePageEvent.UpdateSelectedChatFilter -> {
                state.copy(
                    selectedChatFilterLists = event.list
                )
            }

            is MessagePageEvent.UpdateChatUnreadMessagesNum -> {
                coroutineScope {
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
                coroutineScope {
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
                coroutineScope {
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
                coroutineScope {
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
                coroutineScope {
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

            is MessagePageEvent.UpdateChatNotificationEnabled -> {
                coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.notificationEnabled(event.chatId, event.value)
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

            is MessagePageEvent.ShowChatDetail -> {
                Toast.makeText(context, event.detail.chatId.toString(), Toast.LENGTH_SHORT).show()
                state
            }

            is MessagePageEvent.LoadMessage -> {
                if (state.chatDetail.infoAreaState.expanded) {
                    cancelRealTimeChatCollection()
                }
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        chat = event.chat,
                        editAreaState = state.chatDetail.editAreaState.copy(
                            memeList = refreshMemeList()
                        ),
                        infoAreaState = MessagePageState.InfoAreaState()
                    )
                )
            }


            is MessagePageEvent.OpenEditArea -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            expanded = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.OpenInfoArea -> {
                if (event.open) {
                    state.chatDetail.chat?.let { beginRealTimeChatCollection(it.chatId) }
                } else {
                    cancelRealTimeChatCollection()
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        infoAreaState = state.chatDetail.infoAreaState.copy(
                            expanded = event.open,
                            realTimeChat = state.chatDetail.chat,
                            loadState = LoadState.SUCCESS
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateInfoAreaRealTimeChat -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        infoAreaState = state.chatDetail.infoAreaState.copy(
                            realTimeChat = event.chat,
                            loadState = event.loadState
                        )
                    )
                )
            }

            is MessagePageEvent.AppendEditAreaInput -> {
                state.chatDetail.editAreaState.input.edit {
                    insert(this.selection.end, event.input)
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            iconShrink = when {
                                state.chatDetail.editAreaState.input.text.length > 6 -> true
                                state.chatDetail.editAreaState.input.text.isEmpty() -> false
                                else -> state.chatDetail.editAreaState.iconShrink
                            }
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateEditAreaInput -> {
                state.chatDetail.editAreaState.input.edit {
                    delete(0, length)
                    insert(0, event.input)
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            iconShrink = when {
                                event.input.length > 6 -> true
                                event.input.isEmpty() -> false
                                else -> state.chatDetail.editAreaState.iconShrink
                            }
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateToolbarState -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            toolbarState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateEditAreaMode -> {
                when (event.mode) {
                    EditAreaMode.NORMAL -> {
                        cancelLocationCollection()
                        state.copy(
                            chatDetail = state.chatDetail.copy(
                                editAreaState = state.chatDetail.editAreaState.copy(
                                    mode = event.mode,
                                )
                            )
                        )
                    }

                    EditAreaMode.AUDIO_RECORDER -> {
                        cancelLocationCollection()
                        state.copy(
                            chatDetail = state.chatDetail.copy(
                                editAreaState = state.chatDetail.editAreaState.copy(
                                    mode = event.mode,
                                    audioRecorderState = AudioRecorderState.Ready
                                )
                            )
                        )
                    }

                    EditAreaMode.LOCATION_PICKER -> {
                        beginLocationCollection()
                        state.copy(
                            chatDetail = state.chatDetail.copy(
                                editAreaState = state.chatDetail.editAreaState.copy(
                                    mode = event.mode,
                                    locationPickerState = MessagePageState.LocationPickerState()
                                )
                            )
                        )
                    }
                }
            }

            is MessagePageEvent.UpdateAudioRecorderState -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            audioRecorderState = event.state
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerMenu -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        imagePreviewerState = state.chatDetail.imagePreviewerState.copy(
                            expandMenu = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.ExpandImagePreviewerToolbar -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        imagePreviewerState = state.chatDetail.imagePreviewerState.copy(
                            showToolbar = event.expand
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateImagePreviewerSnapshotList -> {
                viewModelScope.launch {
                    delay(50)
                    sendEffect(MessagePageEffect.ImagePreviewerOpenTransform(event.targetElementIndex))
                }
                state.copy(
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
                state.copy(
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
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            memeList = refreshMemeList()
                        )
                    )
                )
            }

            is MessagePageEvent.SaveImageToLocal -> {
//                fileUtil.saveImageToExternalStorage(event.image.resourceInfo.getModel())
                state
            }

            is MessagePageEvent.ChooseImageUri -> {
                val newList = if (state.chatDetail.editAreaState.chosenImageList.contains(event.imageUri)) {
                    state.chatDetail.editAreaState.chosenImageList.toMutableList().apply {
                        remove(event.imageUri)
                    }
                } else {
                    state.chatDetail.editAreaState.chosenImageList.toMutableList().apply {
                        add(event.imageUri)
                    }
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            chosenImageList = newList
                        ),
                    )
                )
            }

            is MessagePageEvent.ChooseQuoteReply -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            chosenQuoteReply = event.model
                        ),
                    )
                )
            }

            is MessagePageEvent.ShowVoicePermissionDeniedDialog -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            showVoicePermissionDeniedDialog = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.ShowLocationPermissionDeniedDialog -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            showLocationPermissionDeniedDialog = event.open
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateIconShrink -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            iconShrink = event.shouldShrink
                        )
                    )
                )
            }

            is MessagePageEvent.SendMessage -> {
                state.chatDetail.editAreaState.input.edit {
                    delete(0, length)
                }
                viewModelScope.launch {
                    buildParaboxSendMessage()
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            chosenImageList = emptyList(),
                            chosenAtId = null,
                            chosenQuoteReply = null,
                            audioRecorderState = AudioRecorderState.Ready,
                            iconShrink = false,
                            mode = state.chatDetail.editAreaState.mode.takeUnless { it == EditAreaMode.LOCATION_PICKER }
                                ?: EditAreaMode.NORMAL
                        )
                    )
                )
            }

            is MessagePageEvent.SendAudioMessage -> {
                // TODO: send fail notice
                fileUtil.getUriForFile(event.audioFile)?.let {
                    ParaboxAudio(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(it))
                }
                state
            }

            is MessagePageEvent.SendMemeMessage -> {
                ParaboxImage(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(event.imageUri))
                state
            }

            is MessagePageEvent.SendFileMessage -> {
                ParaboxFile(
                    name = event.name,
                    extension = event.name,
                    size = event.size,
                    lastModifiedTime = System.currentTimeMillis(),
                    resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(event.fileUri)
                )
                state
            }

            is MessagePageEvent.AddOrRemoveSelectedMessage -> {
                state.copy(
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
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        selectedMessageList = emptyList()
                    )
                )
            }

            is MessagePageEvent.UpdateLocation -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            locationPickerState = state.chatDetail.editAreaState.locationPickerState.copy(
                                currentLocation = event.location,
                                firstLocationGotten = true
                            )
                        ),
                    )
                )
            }

            is MessagePageEvent.UpdateSelectedLocation -> {
                queryAddressOfSelectedLocation(event.location)
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            locationPickerState = state.chatDetail.editAreaState.locationPickerState.copy(
                                selectedLocation = event.location,
                                selectedLocationAddress = "",
                                selectedLocationAddressLoadState = LoadState.LOADING
                            )
                        ),
                    )
                )
            }

            is MessagePageEvent.UpdateSelectedLocationAddress -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            locationPickerState = state.chatDetail.editAreaState.locationPickerState.copy(
                                selectedLocationAddress = event.address,
                                selectedLocationAddressLoadState = event.loadState
                            )
                        ),
                    )
                )
            }

            is MessagePageEvent.QueryLatestMessageSenderOfChatWithCache -> {
                coroutineScope {
                    withContext(Dispatchers.IO) {
                        val res =
                            try {
                                withTimeout(500) {
                                    suspendCoroutine<Resource<Contact>> { cot ->
                                        getContact.byId(event.senderId).onEach {
                                            if (it is Resource.Success || it is Resource.Error) {
                                                cot.resume(it)
                                            }
                                        }.launchIn(this@coroutineScope)
                                    }
                                }
                            } catch (e: Exception) {
                                Resource.Error("time out")
                            }
                        state.copy(
                            chatLatestMessageSenderCache = state.chatLatestMessageSenderCache.toMutableMap().apply {
                                put(event.senderId, res)
                            }
                        )
                    }
                }
            }

            is MessagePageEvent.QueryAtTargetWithCache -> {
                coroutineScope {
                    withContext(Dispatchers.IO) {
                        val res =
                            try {
                                withTimeout(500) {
                                    suspendCoroutine<Resource<Contact>> { cot ->
                                        getContact.byPlatformInfo(event.pkg, event.uid).onEach {
                                            if (it is Resource.Success || it is Resource.Error) {
                                                cot.resume(it)
                                            }
                                        }.launchIn(this@coroutineScope)
                                    }
                                }
                            } catch (e: Exception) {
                                Resource.Error("time out")
                            }
                        Log.d("parabox", "query at res=$res,name = ${res.data?.name}")
                        state.copy(
                            chatDetail = state.chatDetail.copy(
                                atCache = state.chatDetail.atCache.toMutableMap().apply {
                                    put("${event.pkg}${event.uid}", res)
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private var locationCollectionJob: Job? = null
    private fun beginLocationCollection() {
        locationCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            locationUtil.requestLocationUpdates().collectLatest {
                if (it is Resource.Success) {
                    sendEvent(MessagePageEvent.UpdateLocation(it.data!!))
                }
            }
        }
    }

    private fun cancelLocationCollection() {
        locationCollectionJob?.cancel()
    }

    private fun queryAddressOfSelectedLocation(location: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            locationUtil.getAddressFromLatLng(location)
                .collectLatest {
                    if (it is Resource.Success) {
                        sendEvent(MessagePageEvent.UpdateSelectedLocationAddress(it.data ?: "", LoadState.SUCCESS))
                    } else {
                        sendEvent(MessagePageEvent.UpdateSelectedLocationAddress("", LoadState.ERROR))
                    }
                }
        }
    }

    private fun refreshMemeList(): List<Uri> {
        return FileUtils.listFiles(
            context.getExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_MEME),
            arrayOf("jpg", "jpeg", "png", "gif"), true
        ).reversed().map {
            it.toUri()
        }
    }

    private var realTimeChatCollectionJob: Job? = null
    private fun beginRealTimeChatCollection(chatId: Long) {
        realTimeChatCollectionJob?.cancel()
        realTimeChatCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            getChat.byId(chatId).collectLatest {
                if (it is Resource.Success) {
                    sendEvent(MessagePageEvent.UpdateInfoAreaRealTimeChat(it.data, LoadState.SUCCESS))
                } else if (it is Resource.Error) {
                    sendEvent(
                        MessagePageEvent.UpdateInfoAreaRealTimeChat(
                            uiState.value.chatDetail.infoAreaState.realTimeChat,
                            LoadState.ERROR
                        )
                    )
                }
            }
        }
    }

    private fun cancelRealTimeChatCollection() {
        realTimeChatCollectionJob?.cancel()
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun buildParaboxSendMessage() {
        val contents = buildList<ParaboxMessageElement> {
            if (uiState.value.chatDetail.editAreaState.input.text.isNotEmpty()) {
                add(ParaboxPlainText(uiState.value.chatDetail.editAreaState.input.toString()))
            }
            uiState.value.chatDetail.editAreaState.chosenImageList.forEach {
                add(ParaboxImage(resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(it)))
            }
        }
//        SendMessage(
//            contents = contents,
//
//        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messagePagingDataFlow: Flow<PagingData<ChatPageUiModel>> =
        uiState.distinctUntilChanged { old, new -> old.chatDetail.chat == new.chatDetail.chat || new.chatDetail.chat == null }
            .flatMapLatest {
                it.chatDetail.chat?.let {
                    getMessage(buildList {
                        add(it.chatId)
                        addAll(it.subChatIds)
                    }.distinct(), emptyList())
                } ?: emptyFlow()
            }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>> =
        uiState.distinctUntilChangedBy { it.selectedChatFilterLists }
            .flatMapLatest { getChat(it.selectedChatFilterLists) }.cachedIn(viewModelScope)
    val pinnedChatPagingDataFlow: Flow<PagingData<Chat>> =
        getChat.pinned().cachedIn(viewModelScope)
}