package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.GroupInfoPack
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.use_case.*
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    private val handleNewMessage: HandleNewMessage,
    getContacts: GetContacts,
    val getSenderMessagesOnly: GetSenderMessagesOnly,
    val getMessages: GetMessages,
    val getGroupInfoPack: GetGroupInfoPack,
    val groupNewContact: GroupNewContact,
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

    // Contact
    private val _contactStateFlow: StateFlow<ContactState> =
        getContacts()
            .filter {
                if (it is Resource.Error) {
                    _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar(it.message!!))
                    return@filter false
                } else true
            }
            .map<Resource<List<Contact>>, ContactState> {
                when (it) {
                    is Resource.Loading -> ContactState()
                    is Resource.Success -> {
                        _uiEventFlow.emit(MessagePageUiEvent.UpdateMessageBadge(it.data!!.fold(0) { acc, contact ->
                            acc + (contact.latestMessage?.unreadMessagesNum ?: 0)
                        }))
                        ContactState(
                            isLoading = false,
                            data = it.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> ContactState(isLoading = false)
                }
            }.stateIn(
                initialValue = ContactState(),
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000)
            )
    val contactStateFlow get() = _contactStateFlow

    // Search
    private val _searchBarActivateState = mutableStateOf<Int>(SearchAppBar.NONE)
    val searchBarActivateState : State<Int> = _searchBarActivateState
    fun setSearchBarActivateState(value : Int){
        _searchBarActivateState.value = value
    }
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
    private val _selectedContactIdStateList = mutableStateListOf<Long>()
    val selectedContactIdStateList = _selectedContactIdStateList
    fun addOrRemoveItemOfSelectedContactIdStateList(value: Long) {
        if (!_selectedContactIdStateList.contains(value)) {
            _selectedContactIdStateList.add(value)
        } else {
            _selectedContactIdStateList.remove(value)
        }
    }

    private var _showGroupActionDialogState = mutableStateOf<Boolean>(false)
    val showGroupActionDialogState: State<Boolean> = _showGroupActionDialogState
    fun setShowGroupActionDialogState(value: Boolean) {
        _showGroupActionDialogState.value = value
    }

    private val _groupInfoState = mutableStateOf<GroupInfoState>(GroupInfoState())
    val groupInfoState: State<GroupInfoState> = _groupInfoState
    private var groupInfoJob: Job? = null
    fun getGroupInfoPack() {
        groupInfoJob?.cancel()
        if (selectedContactIdStateList.isEmpty()) {
            _groupInfoState.value =
                GroupInfoState(state = GroupInfoState.ERROR, message = "未选择待编组项")
        } else {
            groupInfoJob = getGroupInfoPack(selectedContactIdStateList.toList()).onEach {
                _groupInfoState.value = GroupInfoState(
                    state = when (it) {
                        is Resource.Error -> GroupInfoState.ERROR
                        is Resource.Loading -> GroupInfoState.LOADING
                        is Resource.Success -> GroupInfoState.SUCCESS
                    }, resource = it.data?.toGroupEditResource(), message = it.message
                )
            }.launchIn(viewModelScope)
        }
    }

    fun groupContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long,
    ) {
        groupInfoJob?.cancel()
        groupNewContact(name, pluginConnections, senderId).onEach {
            _groupInfoState.value = GroupInfoState(
                state = when (it) {
                    is Resource.Error -> GroupInfoState.ERROR
                    is Resource.Loading -> GroupInfoState.LOADING
                    is Resource.Success -> GroupInfoState.NULL
                }, resource = null
            )
            if (it is Resource.Success) {
                _showGroupActionDialogState.value = false
                setSearchBarActivateState(SearchAppBar.NONE)
                clearSelectedContactIdStateList()
                _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar("会话编组成功"))
            }
        }.launchIn(viewModelScope)
    }

//    fun aa(
//        name: String,
//        pluginConnections: List<PluginConnection>,
//        sendId: Long
//    ) {
//        groupInfoJob?.cancel()
//        groupInfoJob = groupNewContact(name, pluginConnections, sendId)
//    }

    fun clearSelectedContactIdStateList() {
        _selectedContactIdStateList.clear()
    }

    // Messages
// Tips: Do Not use contacts from response.
    private val _messageStateFlow = MutableStateFlow(MessageState())
    val messageStateFlow: StateFlow<MessageState> = _messageStateFlow.asStateFlow()
    private var messageJob: Job? = null
    fun receiveAndUpdateMessageFromContact(contact: Contact) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch(Dispatchers.IO) {
            getMessages(contact = contact).collectLatest {
                _messageStateFlow.value = when (it) {
                    is Resource.Loading -> MessageState(
                        state = MessageState.LOADING,
                        profile = contact.profile
                    )
                    is Resource.Error -> MessageState(
                        state = MessageState.ERROR,
                        profile = contact.profile,
                        message = it.message
                    )
                    is Resource.Success -> MessageState(
                        state = MessageState.SUCCESS,
                        profile = contact.profile,
                        data = it.data!!.toTimedMessages()
                    )
                }
            }

        }
    }

//    private val messageStateFlow = MutableStateFlow<ContactWithMessages>()
//    suspend fun getUngroupedMessage(contactId: Int){
//        getUngroupedMessages(contactId).onEach {
//
//        }
//    }

//

    fun testFun() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(PlainText("Hello at ${System.currentTimeMillis()}")),
                    Profile("Ojhdt", null),
                    Profile("Ojhdt-Group", null),
                    System.currentTimeMillis(),
                    PluginConnection(1, 1)
                )
            )
        }
    }

    fun testFun2() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(PlainText("Hi at ${System.currentTimeMillis()}")),
                    Profile("Cool", null),
                    Profile("资源群", null),
                    System.currentTimeMillis(),
                    PluginConnection(1, 2)
                )
            )
        }
    }

    fun testFun3() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(PlainText("Goodbye at ${System.currentTimeMillis()}")),
                    Profile("Steven", null),
                    Profile("课程群", null),
                    System.currentTimeMillis(),
                    PluginConnection(1, 3)
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
