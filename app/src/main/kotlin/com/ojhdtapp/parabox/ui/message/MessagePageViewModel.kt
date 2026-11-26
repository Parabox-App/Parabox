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
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.GetMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.EditAreaMode
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
    val fileUtil: FileUtil,
    val extensionManager: ExtensionManager,
    val mainRepository: MainRepository,
) : BaseViewModel<MessagePageState, MessagePageEvent, MessagePageEffect>() {

    @Volatile
    private var activeFocusedChat: Chat? = null
    private var chatSyncJob: Job? = null
    private val lastChatSyncAtMs = ConcurrentHashMap<Long, Long>()
    private val historySyncController = com.ojhdtapp.parabox.ui.message.chat.ChatHistorySyncController()
    private val historySyncJobs = ConcurrentHashMap<Long, Job>()

    fun onChatFocusEnter(chat: Chat, forceSync: Boolean = false) {
        val previous = activeFocusedChat
        if (previous != null && previous.chatId != chat.chatId) {
            findConnectionSuccess(previous.extensionId)?.notifyChatFocus(previous.uid, opened = false)
        }
        activeFocusedChat = chat
        val connection = findConnectionSuccess(chat.extensionId) ?: return
        connection.notifyChatFocus(chat.uid, opened = true)

        val now = System.currentTimeMillis()
        val lastSynced = lastChatSyncAtMs[chat.chatId] ?: 0L
        if (!forceSync && now - lastSynced < CHAT_ENTRY_SYNC_DEBOUNCE_MS) {
            return
        }
        lastChatSyncAtMs[chat.chatId] = now
        chatSyncJob?.cancel()
        chatSyncJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val latest = connection.syncChatLatestMessages(
                    chatUid = chat.uid,
                    limit = DEFAULT_CHAT_ENTRY_SYNC_LIMIT,
                )
                if (latest.isNotEmpty()) {
                    mainRepository.syncChatMessages(
                        messages = latest,
                        ext = connection,
                        suppressNotification = true,
                        incrementUnread = false,
                    )
                }
            }.onFailure { err ->
                Log.w("parabox", "onChatFocusEnter sync failed for chat ${chat.chatId}: ${err.message}")
            }
        }
    }

    fun onChatFocusExit(chat: Chat) {
        if (activeFocusedChat?.chatId != chat.chatId) return
        activeFocusedChat = null
        chatSyncJob?.cancel()
        chatSyncJob = null
        findConnectionSuccess(chat.extensionId)?.notifyChatFocus(chat.uid, opened = false)
    }

    fun loadOlderHistory(chat: Chat, forceRetry: Boolean = false) {
        val connection = findConnectionSuccess(chat.extensionId) ?: return
        val chatIds = buildList {
            add(chat.chatId)
            addAll(chat.subChatIds)
        }.distinct()
        historySyncJobs[chat.chatId]?.takeIf { it.isActive }?.let { return }
        historySyncJobs[chat.chatId] = viewModelScope.launch(Dispatchers.IO) {
            val oldest = runCatching { getMessage.oldestByChatIds(chatIds) }.getOrNull() ?: return@launch
            if (oldest.uid.isBlank()) return@launch
            val allowed = historySyncController.tryBegin(
                chatId = chat.chatId,
                trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.BOUNDARY,
                anchorUid = oldest.uid,
                anchorTimestamp = oldest.timestamp,
                forceRetry = forceRetry,
            )
            if (!allowed) return@launch
            sendEvent(
                MessagePageEvent.UpdateHistorySyncState(
                    chatId = chat.chatId,
                    status = com.ojhdtapp.parabox.ui.message.chat.HistorySyncStatus.LOADING,
                )
            )
            runCatching {
                val fetched = connection.queryMessageHistory(oldest.uid)
                val inserted = if (fetched.isNotEmpty()) {
                    mainRepository.syncChatMessages(
                        messages = fetched,
                        ext = connection,
                        suppressNotification = true,
                        incrementUnread = false,
                    )
                } else {
                    0
                }
                val newOldest = runCatching { getMessage.oldestByChatIds(chatIds) }.getOrNull()
                val nextStatus = historySyncController.onSuccess(
                    chatId = chat.chatId,
                    trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.BOUNDARY,
                    anchorUid = oldest.uid,
                    anchorTimestamp = oldest.timestamp,
                    returnedCount = fetched.size,
                    insertedCount = inserted,
                    minReturnedTimestamp = fetched.minOfOrNull { it.timestamp },
                    newOldestUid = newOldest?.uid,
                )
                sendEvent(MessagePageEvent.UpdateHistorySyncState(chat.chatId, nextStatus))
            }.onFailure { err ->
                Log.w("parabox", "loadOlderHistory failed for chat ${chat.chatId}: ${err.message}")
                val nextStatus = historySyncController.onFailure(
                    chatId = chat.chatId,
                    trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.BOUNDARY,
                )
                sendEvent(MessagePageEvent.UpdateHistorySyncState(chat.chatId, nextStatus))
            }
        }
    }

    fun healHistoryGapAt(chat: Chat, anchorUid: String, anchorTimestamp: Long) {
        if (anchorUid.isBlank()) return
        val connection = findConnectionSuccess(chat.extensionId) ?: return
        val allowed = historySyncController.tryBegin(
            chatId = chat.chatId,
            trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.GAP_HEAL,
            anchorUid = anchorUid,
            anchorTimestamp = anchorTimestamp,
        )
        if (!allowed) return
        historySyncJobs[chat.chatId] = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val fetched = connection.queryMessageHistory(anchorUid)
                val inserted = if (fetched.isNotEmpty()) {
                    mainRepository.syncChatMessages(
                        messages = fetched,
                        ext = connection,
                        suppressNotification = true,
                        incrementUnread = false,
                    )
                } else {
                    0
                }
                historySyncController.onSuccess(
                    chatId = chat.chatId,
                    trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.GAP_HEAL,
                    anchorUid = anchorUid,
                    anchorTimestamp = anchorTimestamp,
                    returnedCount = fetched.size,
                    insertedCount = inserted,
                    minReturnedTimestamp = fetched.minOfOrNull { it.timestamp },
                    newOldestUid = null,
                )
            }.onFailure { err ->
                Log.w("parabox", "healHistoryGapAt failed for chat ${chat.chatId}: ${err.message}")
                historySyncController.onFailure(
                    chatId = chat.chatId,
                    trigger = com.ojhdtapp.parabox.ui.message.chat.HistorySyncTrigger.GAP_HEAL,
                )
            }
        }
    }

    private fun findConnectionSuccess(extensionId: Long?): Connection.ConnectionSuccess? {
        if (extensionId == null) return null
        return extensionManager.connectionFlow.value
            .firstOrNull { it.connectionId == extensionId } as? Connection.ConnectionSuccess
    }

    override fun onCleared() {
        activeFocusedChat?.let { focused ->
            findConnectionSuccess(focused.extensionId)?.notifyChatFocus(focused.uid, opened = false)
        }
        activeFocusedChat = null
        chatSyncJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_CHAT_ENTRY_SYNC_LIMIT = 30
        private const val CHAT_ENTRY_SYNC_DEBOUNCE_MS = 3_000L
    }

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
                    chatDetail = MessagePageState.ChatDetail(
                        chat = event.chat,
                        editAreaState = MessagePageState.EditAreaState(
                            memeList = refreshMemeList()
                        ),
                        historySyncState = historySyncController.getStatus(event.chat.chatId),
                    )
                )
            }

            is MessagePageEvent.UpdateHistorySyncState -> {
                if (state.chatDetail.chat?.chatId != event.chatId) return state
                return state.copy(
                    chatDetail = state.chatDetail.copy(
                        historySyncState = event.status,
                    )
                )
            }


            is MessagePageEvent.OpenEditArea -> {
                val edit = state.chatDetail.editAreaState
                val exitLocation = !event.open && edit.mode == EditAreaMode.LOCATION_PICKER
                if (exitLocation) cancelLocationCollection()
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = edit.copy(
                            expanded = event.open,
                            mode = if (exitLocation) EditAreaMode.NORMAL else edit.mode,
                        ),
                    ),
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

            is MessagePageEvent.ToggleToolbar -> {
                val edit = state.chatDetail.editAreaState
                cancelLocationCollection()
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = edit.copy(
                            expanded = !edit.expanded || edit.toolbarState != event.toolbar,
                            toolbarState = event.toolbar,
                            mode = EditAreaMode.NORMAL,
                        ),
                    ),
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
                val sendMessage = buildParaboxSendMessage()
                if (sendMessage == null) {
                    sendEffect(MessagePageEffect.ShowSnackBar("消息内容为空或不可用"))
                    return state
                }
                val isLocationMode = state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER
                state.chatDetail.editAreaState.input.edit {
                    delete(0, length)
                }
                dispatchSendMessage(sendMessage = sendMessage, existingMessageId = null)
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            expanded = if (isLocationMode) false else state.chatDetail.editAreaState.expanded,
                            chosenImageList = emptyList(),
                            chosenAtId = null,
                            chosenQuoteReply = null,
                            audioRecorderState = AudioRecorderState.Ready,
                            recordedAudioFile = null,
                            recordedAudioDurationMs = 0L,
                            iconShrink = false,
                            mode = state.chatDetail.editAreaState.mode.takeUnless { it == EditAreaMode.LOCATION_PICKER }
                                ?: EditAreaMode.NORMAL
                        )
                    )
                )
            }

            is MessagePageEvent.SendAudioMessage -> {
                val uri = fileUtil.getUriForFile(event.audioFile)
                if (uri == null || !event.audioFile.exists() || event.audioFile.length() <= 0L) {
                    sendEffect(MessagePageEffect.ShowSnackBar("发送失败：录音文件不可读"))
                    return state
                }
                val durationSec = ((event.durationMs + 500L) / 1000L).coerceAtLeast(1L)
                val audioElement = ParaboxAudio(
                    length = durationSec,
                    fileName = event.audioFile.name,
                    fileSize = event.audioFile.length(),
                    resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri),
                )
                val sendMessage = buildSendMessageWithElements(listOf(audioElement))
                if (sendMessage == null) {
                    sendEffect(MessagePageEffect.ShowSnackBar("发送失败：当前会话无效"))
                    return state
                }
                dispatchSendMessage(sendMessage = sendMessage, existingMessageId = null)
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            audioRecorderState = AudioRecorderState.Ready,
                            recordedAudioFile = null,
                            recordedAudioDurationMs = 0L,
                        )
                    )
                )
            }

            is MessagePageEvent.SendMemeMessage -> {
                val shareableUri = if (event.imageUri.scheme == "file") {
                    event.imageUri.path?.let { path -> fileUtil.getUriForFile(java.io.File(path)) } ?: event.imageUri
                } else {
                    event.imageUri
                }
                val imageElement = ParaboxImage(
                    fileName = fileUtil.getFileName(event.imageUri),
                    resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(shareableUri),
                )
                val sendMessage = buildSendMessageWithElements(listOf(imageElement))
                if (sendMessage == null) {
                    sendEffect(MessagePageEffect.ShowSnackBar("发送失败：当前会话无效"))
                    return state
                }
                dispatchSendMessage(sendMessage = sendMessage, existingMessageId = null)
                state
            }

            is MessagePageEvent.SendFileMessage -> {
                val ext = event.name.substringAfterLast('.', "")
                val mimeType = runCatching { context.contentResolver.getType(event.fileUri) }.getOrNull().orEmpty()
                val element: ParaboxMessageElement = when {
                    mimeType.startsWith("video/") -> ParaboxVideo(
                        fileName = event.name.ifBlank { "video.mp4" },
                        fileSize = event.size,
                        resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(event.fileUri),
                    )
                    mimeType.startsWith("audio/") -> ParaboxAudio(
                        fileName = event.name.ifBlank { "audio.m4a" },
                        fileSize = event.size,
                        resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(event.fileUri),
                    )
                    else -> ParaboxFile(
                        name = event.name.ifBlank { "file" },
                        extension = ext,
                        size = event.size,
                        lastModifiedTime = System.currentTimeMillis(),
                        resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(event.fileUri),
                    )
                }
                val sendMessage = buildSendMessageWithElements(listOf(element))
                if (sendMessage == null) {
                    sendEffect(MessagePageEffect.ShowSnackBar("发送失败：当前会话无效"))
                    return state
                }
                dispatchSendMessage(sendMessage = sendMessage, existingMessageId = null)
                state
            }

            is MessagePageEvent.RetrySendMessage -> {
                val sendMessage = buildSendMessageWithElements(
                    contents = event.message.contents,
                    uuid = event.message.uid.ifBlank { UUID.randomUUID().toString() },
                )
                if (sendMessage == null) {
                    sendEffect(MessagePageEffect.ShowSnackBar("重发失败：当前会话无效"))
                    return state
                }
                dispatchSendMessage(
                    sendMessage = sendMessage,
                    existingMessageId = event.message.messageId,
                )
                state
            }

            is MessagePageEvent.DeleteFailedMessage -> {
                viewModelScope.launch {
                    mainRepository.deleteMessage(event.messageId)
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        sendingStates = state.chatDetail.sendingStates - event.messageId,
                    )
                )
            }

            is MessagePageEvent.UpdateRecordedAudio -> {
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        editAreaState = state.chatDetail.editAreaState.copy(
                            recordedAudioFile = event.file,
                            recordedAudioDurationMs = event.durationMs,
                        )
                    )
                )
            }

            is MessagePageEvent.UpdateMessageSendStatus -> {
                val nextMap = state.chatDetail.sendingStates.toMutableMap().apply {
                    if (event.status == null) {
                        remove(event.messageId)
                    } else {
                        put(event.messageId, event.status)
                    }
                }
                state.copy(
                    chatDetail = state.chatDetail.copy(
                        sendingStates = nextMap,
                    )
                )
            }

            is MessagePageEvent.ShowSendErrorNotice -> {
                sendEffect(MessagePageEffect.ShowSnackBar(event.message))
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

    fun buildParaboxSendMessage(): SendMessage? {
        val editState = uiState.value.chatDetail.editAreaState
        val contents = when (editState.mode) {
            EditAreaMode.LOCATION_PICKER -> {
                val loc = editState.locationPickerState.selectedLocation
                if (loc.latitude == 0.0 && loc.longitude == 0.0 && !editState.locationPickerState.firstLocationGotten) {
                    return null
                }
                listOf(
                    ParaboxLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        name = editState.locationPickerState.selectedLocationAddress.ifBlank { null },
                        description = null,
                    )
                )
            }
            EditAreaMode.AUDIO_RECORDER -> {
                val audioFile = editState.recordedAudioFile ?: return null
                val uri = fileUtil.getUriForFile(audioFile) ?: return null
                if (!audioFile.exists() || audioFile.length() <= 0L) return null
                val durationSec = ((editState.recordedAudioDurationMs + 500L) / 1000L).coerceAtLeast(1L)
                listOf(
                    ParaboxAudio(
                        length = durationSec,
                        fileName = audioFile.name,
                        fileSize = audioFile.length(),
                        resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri),
                    )
                )
            }
            EditAreaMode.NORMAL -> buildList<ParaboxMessageElement> {
                editState.chosenQuoteReply?.let { quote ->
                    add(
                        ParaboxQuoteReply(
                            sender = ParaboxContact(
                                basicInfo = ParaboxBasicInfo(
                                    name = quote.sender.name,
                                    avatar = quote.sender.avatar,
                                ),
                                uid = quote.sender.uid,
                            ),
                            timestamp = quote.message.timestamp,
                            id = quote.message.uid,
                            messages = quote.message.contents,
                        )
                    )
                }
                if (editState.input.text.isNotEmpty()) {
                    add(ParaboxPlainText(editState.input.text.toString()))
                }
                editState.chosenImageList.forEach { uri ->
                    val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
                    val fileName = fileUtil.getFileName(uri)
                    if (mimeType.startsWith("video/")) {
                        add(
                            ParaboxVideo(
                                fileName = fileName ?: "video.mp4",
                                resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri),
                            )
                        )
                    } else {
                        add(
                            ParaboxImage(
                                fileName = fileName,
                                resourceInfo = ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri),
                            )
                        )
                    }
                }
            }
        }
        return buildSendMessageWithElements(contents)
    }

    private fun buildSendMessageWithElements(
        contents: List<ParaboxMessageElement>,
        uuid: String = UUID.randomUUID().toString(),
    ): SendMessage? {
        if (contents.isEmpty()) return null
        return uiState.value.chatDetail.chat?.let { currentChat ->
            SendMessage(
                contents = contents,
                chat = ParaboxChat(
                    basicInfo = ParaboxBasicInfo(
                        name = currentChat.name,
                        avatar = currentChat.avatar
                    ),
                    type = currentChat.type,
                    uid = currentChat.uid
                ),
                timestamp = System.currentTimeMillis(),
                uuid = uuid,
            )
        }
    }

    private fun dispatchSendMessage(
        sendMessage: SendMessage,
        existingMessageId: Long?,
    ) {
        val currentChat = uiState.value.chatDetail.chat ?: run {
            sendEffect(MessagePageEffect.ShowSnackBar("发送失败：当前会话无效"))
            return
        }
        val extensionId = currentChat.extensionId ?: run {
            sendEffect(MessagePageEffect.ShowSnackBar("发送失败：当前会话未关联扩展"))
            return
        }
        viewModelScope.launch {
            val localMessageId = if (existingMessageId != null) {
                mainRepository.resetMessageForRetry(existingMessageId, sendMessage.timestamp)
                existingMessageId
            } else {
                mainRepository.insertOutgoingMessage(
                    msg = sendMessage,
                    pkg = currentChat.pkg,
                    extensionId = extensionId,
                    chatId = currentChat.chatId,
                )
            }

            if (localMessageId <= 0L) {
                sendEffect(MessagePageEffect.ShowSnackBar("发送失败：本地消息写入异常"))
                return@launch
            }

            sendEvent(
                MessagePageEvent.UpdateMessageSendStatus(
                    messageId = localMessageId,
                    status = MessagePageState.MessageSendStatus.Sending,
                )
            )

            val connection = extensionManager.connectionFlow.value
                .firstOrNull { it.connectionId == currentChat.extensionId }

            val result: ParaboxResult = when (connection) {
                is Connection.ConnectionSuccess -> {
                    try {
                        connection.sendMessage(sendMessage)
                    } catch (e: Exception) {
                        Log.e("parabox", "sendMessage threw exception", e)
                        ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: "发送异常")
                    }
                }
                is Connection.ConnectionFail -> {
                    val detail = connection.reason.ifBlank { "未知原因" }
                    ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "扩展连接异常：$detail")
                }
                is Connection.ConnectionPending -> {
                    ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "扩展正在连接中，请稍后重试")
                }
                null -> {
                    ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "未找到对应的扩展连接")
                }
            }

            if (result.code == ParaboxResult.SUCCESS) {
                mainRepository.markMessageSendSuccess(localMessageId, result.messageUuid)
                sendEvent(
                    MessagePageEvent.UpdateMessageSendStatus(
                        messageId = localMessageId,
                        status = MessagePageState.MessageSendStatus.Sent,
                    )
                )
            } else {
                val reason = result.message.ifBlank { "未知错误 (${result.code})" }
                sendEvent(
                    MessagePageEvent.UpdateMessageSendStatus(
                        messageId = localMessageId,
                        status = MessagePageState.MessageSendStatus.Failed(reason),
                    )
                )
                val retryMessage = Message(
                    contents = sendMessage.contents,
                    contentTypes = 0,
                    contentString = sendMessage.contents.joinToString(" ") { it.contentToString() },
                    senderId = 0L,
                    chatId = currentChat.chatId,
                    timestamp = sendMessage.timestamp,
                    sentByMe = true,
                    verified = false,
                    pkg = currentChat.pkg,
                    uid = sendMessage.uuid,
                    extensionId = extensionId,
                    messageId = localMessageId,
                )
                sendEffect(
                    MessagePageEffect.ShowSnackBar(
                        message = "发送失败：$reason",
                        label = "重试",
                        callback = {
                            sendEvent(MessagePageEvent.RetrySendMessage(retryMessage))
                        },
                    )
                )
            }
        }
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
    val contactPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>> =
        uiState.distinctUntilChanged { old, new -> old.chatDetail.chat == new.chatDetail.chat || new.chatDetail.chat == null }
            .flatMapLatest {
                it.chatDetail.chat?.let {
                    getContact.inChatPagingSource(buildList {
                        add(it.chatId)
                        addAll(it.subChatIds)
                    }.distinct())
                } ?: emptyFlow()
            }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>> =
        uiState.distinctUntilChangedBy { it.selectedChatFilterLists }
            .flatMapLatest { getChat(it.selectedChatFilterLists) }.cachedIn(viewModelScope)
    val pinnedChatPagingDataFlow: Flow<PagingData<Chat>> =
        getChat.pinned().cachedIn(viewModelScope)
}