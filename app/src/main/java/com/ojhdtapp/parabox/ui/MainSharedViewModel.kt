package com.ojhdtapp.parabox.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkInfo
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.At
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.model.*
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.GetMessages
import com.ojhdtapp.parabox.domain.use_case.GroupNewContact
import com.ojhdtapp.parabox.domain.use_case.UpdateContact
import com.ojhdtapp.parabox.ui.message.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.MessageState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainSharedViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val getMessages: GetMessages,
    private val deleteMessage: DeleteMessage,
    private val groupNewContact: GroupNewContact,
    private val updateContact: UpdateContact,
) : ViewModel() {

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<MainSharedUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    // Activity Navigate
    fun navigateToChatPage(targetContact: Contact) {
        clearContactUnreadNum(targetContact.contactId)
        loadMessageFromContact(targetContact)
        viewModelScope.launch {
            delay(500)
            _uiEventFlow.emit(MainSharedUiEvent.NavigateToChat(targetContact))
        }
    }

    fun navigateToChatPage(targetContact: Contact, message: Message) {
        clearContactUnreadNum(targetContact.contactId)
        loadMessageFromContact(targetContact)
        viewModelScope.launch {
            delay(500)
            _uiEventFlow.emit(MainSharedUiEvent.NavigateToChat(targetContact))
            delay(500)
            _uiEventFlow.emit(MainSharedUiEvent.NavigateToChatMessage(message))
        }
    }

    // Badge
    private val _messageBadge = mutableStateOf<Int>(0)
    val messageBadge: State<Int> = _messageBadge
    fun setMessageBadge(value: Int) {
        _messageBadge.value = value
    }

    // Messages
    // Tips: Do Not use contacts from response.
    private val _messageStateFlow = MutableStateFlow(MessageState())
    val messageStateFlow: StateFlow<MessageState> = _messageStateFlow.asStateFlow()

    fun clearContactUnreadNum(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.unreadMessagesNum(contactId, 0)
            updateContact.hiddenState(contactId, false)
        }
    }

    fun loadMessageFromContact(contact: Contact) {
        clearQuoteMessage()
        _messageStateFlow.value = MessageState()
        _selectedMessageStateList.clear()
        _messageStateFlow.value = MessageState(MessageState.LOADING, contact)
        viewModelScope.launch(Dispatchers.IO) {
            getMessages.pluginConnectionList(contact).also {
                _messageStateFlow.value = MessageState(
                    MessageState.SUCCESS,
                    contact,
                    it,
                    it.findLast { it.objectId == contact.senderId })
            }
        }
    }

    fun updateSelectedPluginConnection(plg: PluginConnection) {
        _messageStateFlow.value = messageStateFlow.value.copy(
            selectedPluginConnection = plg
        )
    }

    fun receiveMessagePagingDataFlow(pluginConnectionObjectIdList: List<Long>): Flow<PagingData<Message>> {
        return getMessages.pagingFlow(pluginConnectionObjectIdList)
            .onEach {
                viewModelScope.launch(Dispatchers.IO) {
                    messageStateFlow.value.contact?.contactId?.let {
                        updateContact.unreadMessagesNum(it, 0)
                    }
                }
            }
            .cachedIn(viewModelScope)
    }


    fun clearMessage() {
        _messageStateFlow.value = MessageState()
        _selectedMessageStateList.clear()
    }

    // New Contact
    private val _sendTargetType = mutableStateOf<Int>(SendTargetType.USER)
    val sendTargetType: State<Int> = _sendTargetType
    fun setSendTargetType(value: Int) {
        _sendTargetType.value = value
    }

    private val _selectedExtensionId = mutableStateOf<Int?>(null)
    val selectedExtensionId: State<Int?> = _selectedExtensionId
    fun setSelectedExtensionId(value: Int?) {
        _selectedExtensionId.value = value
    }

    private val _idInput = mutableStateOf<String>("")
    val idInput: State<String> = _idInput
    fun setIdInput(value: String) {
        _idInput.value = value
    }

    fun groupContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long,
        avatar: String? = null,
        avatarUri: String? = null,
        tags: List<String> = emptyList(),
        contactId: Long? = null,
    ) {
        groupNewContact(
            name,
            pluginConnections,
            senderId,
            avatar,
            avatarUri,
            tags,
            contactId
        ).onEach {
            when (it) {
                is Resource.Success -> {
                    _uiEventFlow.emit(MainSharedUiEvent.BottomSheetControl(false))
                    _selectedExtensionId.value = null
                    _sendTargetType.value = SendTargetType.USER
                    _idInput.value = ""
                    Toast.makeText(context, "新建会话成功", Toast.LENGTH_SHORT).show()
                }

                is Resource.Error -> {
                    Toast.makeText(context, "会话编组失败", Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }.launchIn(viewModelScope)
    }

    // Selection
    private val _selectedMessageStateList = mutableStateListOf<Message>()
    val selectedMessageStateList = _selectedMessageStateList
    fun addOrRemoveItemOfSelectedMessageStateList(value: Message) {
        if (!_selectedMessageStateList.contains(value)) {
            _selectedMessageStateList.add(value)
        } else {
            _selectedMessageStateList.remove(value)
        }
    }

    fun clearSelectedMessageStateList() {
        _selectedMessageStateList.clear()
    }

    fun deleteMessage(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteMessage(messageIdList = ids)
        }
        clearSelectedMessageStateList()
    }

    // Plugin List
    private val _pluginListStateFlow = MutableStateFlow<List<AppModel>>(emptyList())
    val pluginListStateFlow = _pluginListStateFlow.asStateFlow()
    fun setPluginListStateFlow(value: List<AppModel>) {
        _pluginListStateFlow.value = value
    }

    // Quote Message
    private val _quoteMessageState = mutableStateOf<Message?>(null)
    val quoteMessageState: State<Message?> = _quoteMessageState
    fun setQuoteMessage(value: Message?, name: String? = null) {
        if (value?.sentByMe == true && name != null) {
            _quoteMessageState.value = value.copy(
                profile = Profile(name = name, null, null, null)
            )
        } else {
            _quoteMessageState.value = value
        }
        if (value != null && _atState.value?.target != value.profile.id) {
            clearAt()
        }
    }

    fun clearQuoteMessage() {
        _quoteMessageState.value = null
    }

    // At
    private val _atState = mutableStateOf<At?>(null)
    val atState: State<At?> = _atState
    fun setAtState(value: At?) {
        _atState.value = value
        if (value != null && value.target != _quoteMessageState.value?.profile?.id) {
            clearQuoteMessage()
        }
    }

    fun clearAt() {
        _atState.value = null
    }

    // User name
    val userNameFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.USER_NAME] ?: DataStoreKeys.DEFAULT_USER_NAME
        }

    fun setUserName(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[DataStoreKeys.USER_NAME] = value
            }
        }
    }

    // User Avatar
    val userAvatarFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.USER_AVATAR]
        }

    // Profile Dialog
    private val _showUserProfileDialogState = mutableStateOf<Boolean>(false)
    val showUserProfileDialogState: State<Boolean> = _showUserProfileDialogState
    fun setShowUserProfileDialogState(value: Boolean) {
        _showUserProfileDialogState.value = value
    }

    // Edit Username Dialog
    private val _editUserNameDialogState = mutableStateOf<Boolean>(false)
    val editUserNameDialogState: State<Boolean> = _editUserNameDialogState
    fun setEditUserNameDialogState(value: Boolean) {
        _editUserNameDialogState.value = value
    }

    // Record State
    private val _audioRecorderState = mutableStateOf<AudioRecorderState>(AudioRecorderState.Ready)
    val audioRecorderState: State<AudioRecorderState> = _audioRecorderState
    fun setAudioRecorderState(value: AudioRecorderState) {
        _audioRecorderState.value = value
    }

    // Record Amplitude
    private val _recordAmplitudeStateList = mutableStateListOf<Int>()
    val recordAmplitudeStateList get() = _recordAmplitudeStateList
    fun clearRecordAmplitudeStateList() {
        _recordAmplitudeStateList.clear()
    }

    fun insertIntoRecordAmplitudeStateList(item: Int) {
        _recordAmplitudeStateList.add(item)
        setAudioPlayerProgressFraction((_recordAmplitudeStateList.size + 1) * 2f)
    }

    fun insertAllIntoRecordAmplitudeStateList(list: List<Int>) {
        clearRecordAmplitudeStateList()
        _recordAmplitudeStateList.addAll(list)
    }

    // Player Progress
    private val _isAudioPlaying = mutableStateOf<Boolean>(false)
    val isAudioPlaying: State<Boolean> = _isAudioPlaying
    fun setIsAudioPlaying(value: Boolean) {
        _isAudioPlaying.value = value
    }

    private val _audioPlayerProgressFraction = mutableStateOf<Float>(0f)
    val audioPlayerProgressFraction: State<Float> = _audioPlayerProgressFraction
    fun setAudioPlayerProgressFraction(value: Float) {
        _audioPlayerProgressFraction.value = value
    }

    // Swipe Refresh
    private val _isRefreshing = mutableStateOf<Boolean>(false)
    val isRefreshing: State<Boolean> = _isRefreshing
    fun setIsRefreshing(value: Boolean) {
        _isRefreshing.value = value
    }

    val cloudServiceFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_CLOUD_SERVICE] ?: 0
        }

    fun setCloudService(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = value
            }
        }
    }

    val cloudTotalSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_TOTAL_SPACE] ?: 0L
        }

    val cloudUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_USED_SPACE] ?: 0L
        }
    val cloudAppUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_APP_USED_SPACE] ?: 0L
        }

    fun saveGoogleDriveAccount(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.GOOGLE_NAME] = account?.displayName ?: ""
                preferences[DataStoreKeys.GOOGLE_MAIL] = account?.email ?: ""
                preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = if(account == null) 0 else GoogleDriveUtil.SERVICE_CODE
                preferences[DataStoreKeys.GOOGLE_AVATAR] = account?.photoUrl.toString()
            }
            GoogleDriveUtil.getDriveInformation(context)?.also {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.GOOGLE_WORK_FOLDER_ID] = it.workFolderId
                    preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.totalSpace
                    preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.usedSpace
                    preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = it.appUsedSpace
                }
            }
        }
    }

    private val _workInfoMap = mutableStateMapOf<String, Pair<File, List<WorkInfo>>>()
    val workInfoMap get() = _workInfoMap
    fun putWorkInfo(tag: String, file: File, workInfoList: List<WorkInfo>) {
        _workInfoMap[tag] = file to workInfoList
    }

    // WorkInfo
    private val _workInfoDialogState = mutableStateOf<Boolean>(false)
    val workInfoDialogState: State<Boolean> = _workInfoDialogState
    fun setWorkInfoDialogState(value: Boolean) {
        _workInfoDialogState.value = value
    }

    private val _workInfoStateFlow = MutableStateFlow<Map<File, List<WorkInfo>>>(emptyMap())
    val workInfoStateFlow get() = _workInfoStateFlow.asStateFlow()
    fun setWorkInfoStateFlow(value: Map<File, List<WorkInfo>>) {
        _workInfoStateFlow.value = value
    }

    // translation
    val translationFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ML_KIT_TRANSLATION] ?: true
        }

    // First Launch
    private val _guideLaunchedStateFlow = MutableStateFlow(false)
    val guideLaunchedStateFlow = _guideLaunchedStateFlow.asStateFlow()

    fun launchedGuide() {
        _guideLaunchedStateFlow.value = true
    }
}
