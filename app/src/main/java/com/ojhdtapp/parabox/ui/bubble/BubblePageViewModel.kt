package com.ojhdtapp.parabox.ui.bubble

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.GetMessages
import com.ojhdtapp.parabox.ui.message.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.MessageState
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.At
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BubblePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val getMessages: GetMessages,
    private val deleteMessage: DeleteMessage,
) : ViewModel() {
    // Messages
    // Tips: Do Not use contacts from response.
    private val _messageStateFlow = MutableStateFlow(MessageState())
    val messageStateFlow: StateFlow<MessageState> = _messageStateFlow.asStateFlow()

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
            .cachedIn(viewModelScope)
    }


    fun clearMessage() {
        _messageStateFlow.value = MessageState()
        _selectedMessageStateList.clear()
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
}