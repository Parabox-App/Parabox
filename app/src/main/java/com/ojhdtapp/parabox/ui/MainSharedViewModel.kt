package com.ojhdtapp.parabox.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.model.*
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.GetMessages
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
) : ViewModel() {

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
    val quoteMessageState : State<Message?> = _quoteMessageState
    fun setQuoteMessage(value : Message?, name: String? = null){
        if(value?.sentByMe == true && name != null){
            _quoteMessageState.value = value.copy(
                profile = Profile(name = name, null, null)
            )
        } else {
            _quoteMessageState.value = value
        }
    }
    fun clearQuoteMessage(){
        _quoteMessageState.value = null
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
            settings[DataStoreKeys.USER_NAME] ?: "User"
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
}
