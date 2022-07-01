package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.use_case.GetGroupedMessages
import com.ojhdtapp.parabox.domain.use_case.GetUngroupedContactList
import com.ojhdtapp.parabox.domain.use_case.GetUngroupedMessages
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    private val handleNewMessage: HandleNewMessage,
    getUngroupedContactList: GetUngroupedContactList,
    val getUngroupedMessages: GetUngroupedMessages,
    val getGroupedMessages: GetGroupedMessages,
) : ViewModel() {
    init {
        // Update Ungrouped Contacts
//        getUngroupedContactList().onEach {
//            Log.d("parabox", "contactList:${it}")
//            when (it) {
//                is Resource.Loading -> {
//                    setUngroupedContactState(
//                        ungroupedContactState.value.copy(
//                            isLoading = true,
//                        )
//                    )
//                }
//                is Resource.Error -> {
//                    setUngroupedContactState(ungroupedContactState.value.copy(isLoading = false))
//                    _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar(it.message!!))
//                }
//                is Resource.Success -> {
//                    setUngroupedContactState(
//                        ungroupedContactState.value.copy(
//                            isLoading = false,
//                            data = it.data!!
//                        )
//                    )
//                    updateMessageBadge(it.data.sumOf { contact ->
//                        contact.latestMessage?.unreadMessagesNum ?: 0
//                    })
//                }
//            }
//        }.catch {
//            _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar("获取数据时发生错误"))
//        }.launchIn(viewModelScope)
    }

    fun onEvent(event: MessagePageEvent) {
        when (event) {

            else -> {

            }
        }
    }

    // emit to this when wanting toasting
    private val _uiEventFlow = MutableSharedFlow<MessagePageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    fun showSnackBar(message: String) {
        viewModelScope.launch {
            _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar(message = message))
        }
    }

    // Ungrouped Contact
//    private val _ungroupedContactState =
//        mutableStateOf<UngroupedContactState>(UngroupedContactState())
//    val ungroupedContactState: State<UngroupedContactState> = _ungroupedContactState
//    fun setUngroupedContactState(value: UngroupedContactState) {
//        _ungroupedContactState.value = value
//    }
    private val _ungroupedContactStateFlow: StateFlow<UngroupedContactState> =
        getUngroupedContactList()
            .filter {
                if (it is Resource.Error) {
                    _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar(it.message!!))
                    return@filter false
                } else true
            }
            .map<Resource<List<Contact>>, UngroupedContactState> {
                when (it) {
                    is Resource.Loading -> UngroupedContactState()
                    is Resource.Success -> UngroupedContactState(
                        isLoading = false,
                        data = it.data ?: emptyList()
                    )
                    is Resource.Error -> UngroupedContactState(isLoading = false)
                }
            }.stateIn(
                initialValue = UngroupedContactState(),
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000)
            )
    val ungroupedContactStateFlow get() = _ungroupedContactStateFlow

    // Search
    private val _searchText = mutableStateOf<String>("")
    val searchText: State<String> = _searchText
    fun setSearchText(value: String) {
        _searchText.value = value
    }

    // Badge
    private suspend fun updateMessageBadge(value: Int) {
        _uiEventFlow.emit(MessagePageUiEvent.UpdateMessageBadge(value))
    }

    // Selection
    private val _selectedContactIdStateList = mutableStateListOf<Int>()
    val selectedContactIdStateList = _selectedContactIdStateList
    fun addOrRemoveItemOfSelectedContactIdStateList(value: Int) {
        if (!_selectedContactIdStateList.contains(value)) {
            _selectedContactIdStateList.add(value)
        } else {
            _selectedContactIdStateList.remove(value)
        }
    }

    fun clearSelectedContactIdStateList() {
        _selectedContactIdStateList.clear()
    }

    // Messages
    private val _messageState = mutableStateOf<MessageState>(MessageState())
    val messageState: State<MessageState> = _messageState
    fun setMessageState(value: MessageState) {
        _messageState.value = value
    }
//    private val messageStateFlow = MutableStateFlow<ContactWithMessages>()
//    suspend fun getUngroupedMessage(contactId: Int){
//        getUngroupedMessages(contactId).onEach {
//
//        }
//    }

    fun testFun() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(PlainText("Hello at ${System.currentTimeMillis()}")),
                    Profile("Ojhdt", null),
                    Profile("Ojhdt-Group", null),
                    System.currentTimeMillis().toInt(),
                    System.currentTimeMillis(),
                    PluginConnection(1, 1)
                )
            )
        }
    }


    private val _pluginInstalledState = mutableStateOf(false)
    val pluginInstalledState = _pluginInstalledState
    fun setPluginInstalledState(value: Boolean) {
        _pluginInstalledState.value = value
    }

    private val _sendAvailableState = mutableStateOf<Boolean>(false)
    val sendAvailableState: State<Boolean> = _sendAvailableState

    fun setSendAvailableState(value: Boolean) {
        _sendAvailableState.value = value
    }

    private val _message = mutableStateOf<String>("Text")
    val message: State<String> = _message

    fun setMessage(value: String) {
        _message.value = value
    }
}
