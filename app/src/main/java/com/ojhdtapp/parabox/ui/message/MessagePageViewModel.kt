package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.Tags
import com.ojhdtapp.messagedto.MessageDto
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.use_case.*
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    private val handleNewMessage: HandleNewMessage,
    getContacts: GetContacts,
    val getGroupInfoPack: GetGroupInfoPack,
    val groupNewContact: GroupNewContact,
    val updateContact: UpdateContact,
    val tagControl: TagControl,
    val getArchivedContacts: GetArchivedContacts,
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

    private val _areaState = mutableStateOf<Int>(AreaState.MessageArea)
    val areaState : State<Int> = _areaState
    fun setAreaState(value : Int){
        _areaState.value = value
    }

    // Contact
    private val _contactRefreshFlow = MutableStateFlow<Long>(0L)
    private val _contactStateFlow: StateFlow<ContactState> =
        getContacts()
            .combine(_contactRefreshFlow) { contacts, refresh ->
                contacts
            }
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
                            data = it.data.filter {
                                typeFilter.value.contactCheck(it)
                                        && readFilter.value.contactCheck(it)
                                        && if (selectedContactTagStateList.isNotEmpty()) {
                                    (selectedContactTagStateList intersect it.tags.toSet()).isNotEmpty()
                                } else true
                            }.sortedByDescending { it.isPinned }
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

    private val _archivedContactStateFlow: StateFlow<List<Contact>> =
        getArchivedContacts().filter {
            if (it is Resource.Error) {
                _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar(it.message!!))
                return@filter false
            } else true
        }.map<Resource<List<Contact>>, List<Contact>> {
            when(it){
                is Resource.Loading -> emptyList()
                is Resource.Success -> {
                    showArchiveContact()
                    it.data!!
                }
                else -> emptyList()
            }
        }.stateIn(
            initialValue = emptyList<Contact>(),
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )
    val archivedContactStateFlow get() = _archivedContactStateFlow

    private val _archivedContactHidden = mutableStateOf<Boolean>(false)
    val archivedContactHidden : State<Boolean> = _archivedContactHidden
    fun hideArchiveContact(){
        _archivedContactHidden.value = true
    }
    fun showArchiveContact(){
        _archivedContactHidden.value = false
    }

    fun refreshContactStateFlow() {
        viewModelScope.launch {
            _contactRefreshFlow.emit(System.currentTimeMillis())
        }
    }

    // Search
    private val _searchBarActivateState = mutableStateOf<Int>(SearchAppBar.NONE)
    val searchBarActivateState: State<Int> = _searchBarActivateState
    fun setSearchBarActivateState(value: Int) {
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
    private val _selectedContactStateList = mutableStateListOf<Contact>()
    val selectedContactStateList = _selectedContactStateList
    fun addOrRemoveItemOfSelectedContactStateList(value: Contact) {
        if (!_selectedContactStateList.map { it.contactId }.contains(value.contactId)) {
            _selectedContactStateList.add(value)
        } else {
            _selectedContactStateList.remove(_selectedContactStateList.findLast { it.contactId == value.contactId })
        }
    }

    fun clearSelectedContactStateList() {
        _selectedContactStateList.clear()
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
        if (selectedContactStateList.isEmpty()) {
            _groupInfoState.value =
                GroupInfoState(state = GroupInfoState.ERROR, message = "未选择待编组项")
        } else {
            groupInfoJob =
                getGroupInfoPack(selectedContactStateList.map { it.contactId }.toList()).onEach {
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
                clearSelectedContactStateList()
                _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar("会话编组成功"))
            }
        }.launchIn(viewModelScope)
    }

    var updateHiddenStateJob: Job? = null
    var tempContactIdForHiddenCancellation = mutableListOf<Long>()
    fun setContactHidden(contactId: Long) {
        tempContactIdForHiddenCancellation.clear()
        tempContactIdForHiddenCancellation.add(contactId)
        updateHiddenStateJob = viewModelScope.launch(Dispatchers.IO) {
//            _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar("会话已暂时隐藏", "取消"))
            delay(200)
            updateContact.hiddenState(contactId, true)
        }
    }

    fun setContactHidden(contactIds: List<Long>) {
        tempContactIdForHiddenCancellation.clear()
        tempContactIdForHiddenCancellation.addAll(contactIds)
        updateHiddenStateJob = viewModelScope.launch(Dispatchers.IO) {
//            _uiEventFlow.emit(MessagePageUiEvent.ShowSnackBar("会话已暂时隐藏", "取消"))
            delay(200)
            contactIds.forEach {
                updateContact.hiddenState(it, true)
            }
        }
    }

    fun cancelContactHidden() {
        if (tempContactIdForHiddenCancellation.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                tempContactIdForHiddenCancellation.forEach {
                    updateContact.hiddenState(it, false)
                }
            }
        }
    }

    fun setContactTag(contactId: Long, tags: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.tag(contactId, tags)
        }
    }

    fun setContactPinned(contactId: Long, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.pinnedState(contactId, pinned)
        }
    }

    fun setContactPinned(contactIds: List<Long>, pinned: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            contactIds.forEach {
                updateContact.pinnedState(it, pinned)
            }
        }
    }

    fun setContactNotification(contactId: Long, enableNotification: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.notificationState(contactId, enableNotification)
        }
    }

    fun setContactNotification(contactIds: List<Long>, enableNotification: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            contactIds.forEach {
                updateContact.notificationState(it, enableNotification)
            }
        }
    }

    fun setContactProfileAndTag(contactId: Long, profile: Profile, tags: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.profileAndTag(contactId, profile, tags)
        }
    }

    fun setContactArchived(contactId: Long, isArchived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.archivedState(contactId, isArchived)
        }
    }

    fun setContactArchived(contactIds: List<Long>, isArchived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            contactIds.forEach {
                updateContact.archivedState(it, isArchived)
            }
        }
    }

    fun clearContactUnreadNum(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.unreadMessagesNum(contactId, 0)
        }
    }

    fun clearContactUnreadNum(contactIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactIds.forEach {
                updateContact.unreadMessagesNum(it, 0)
            }
        }
    }

    fun restoreContactUnreadNum(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.unreadMessagesNum(contactId, 1)
        }
    }

    fun restoreContactUnreadNum(contactIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            contactIds.forEach {
                updateContact.unreadMessagesNum(it, 1)
            }
        }
    }

    // Edit Dialog
    private var _showEditActionDialogState = mutableStateOf<Boolean>(false)
    val showEditActionDialogState: State<Boolean> = _showEditActionDialogState
    fun setShowEditActionDialogState(value: Boolean) {
        _showEditActionDialogState.value = value
    }

    // Tag Dialog
    private val _showTagEditAlertDialogState = mutableStateOf<Boolean>(false)
    val showTagEditAlertDialogState: State<Boolean> = _showTagEditAlertDialogState
    fun setShowTagEditAlertDialogState(value: Boolean) {
        _showTagEditAlertDialogState.value = value
    }

    // Tag & Filter
    private val _tagEditing = mutableStateOf<Boolean>(false)
    val tagEditing: State<Boolean> = _tagEditing
    fun setTagEditing(value: Boolean) {
        _tagEditing.value = value
    }

    private val _contactTagStateFlow = tagControl.get().stateIn(
        initialValue = emptyList(),
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )
    val contactTagStateFlow get() = _contactTagStateFlow
    fun addContactTag(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagControl.add(value)
        }
    }

    fun addContactTag(value: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            value.forEach {
                if (!tagControl.has(it))
                    tagControl.add(it)
            }
        }
    }

    fun deleteContactTag(value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tagControl.delete(value)
        }
    }

    private val _selectedContactTagStateList = mutableStateListOf<String>()
    val selectedContactTagStateList = _selectedContactTagStateList
    fun addOrRemoveItemOfSelectedContactTagStateList(value: String) {
        if (!_selectedContactTagStateList.contains(value)) {
            _selectedContactTagStateList.add(value)
        } else {
            _selectedContactTagStateList.remove(value)
        }
        refreshContactStateFlow()
    }

    fun clearSelectedContactTagStateList() {
        _selectedContactTagStateList.clear()
        refreshContactStateFlow()
    }

    private val _readFilter = mutableStateOf<ContactReadFilterState>(ContactReadFilterState.All())
    val readFilter: State<ContactReadFilterState> = _readFilter
    fun setReadFilter(value: ContactReadFilterState) {
        _readFilter.value = value
        refreshContactStateFlow()
    }

    private val _typeFilter = mutableStateOf<ContactTypeFilterState>(ContactTypeFilterState.All())
    val typeFilter: State<ContactTypeFilterState> = _typeFilter
    fun setTypeFilter(value: ContactTypeFilterState) {
        _typeFilter.value = value
        refreshContactStateFlow()
    }

    fun testFun() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(com.ojhdtapp.messagedto.message_content.PlainText("Hello at ${System.currentTimeMillis()}")),
                    com.ojhdtapp.messagedto.Profile("Ojhdt", null),
                    com.ojhdtapp.messagedto.Profile("Ojhdt-Group", null),
                    System.currentTimeMillis(),
                    com.ojhdtapp.messagedto.PluginConnection(1, 1, 1)
                )
            )
        }
    }

    fun testFun2() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(com.ojhdtapp.messagedto.message_content.PlainText("Hi at ${System.currentTimeMillis()}")),
                    com.ojhdtapp.messagedto.Profile("Cool", null),
                    com.ojhdtapp.messagedto.Profile("资源群", null),
                    System.currentTimeMillis(),
                    com.ojhdtapp.messagedto.PluginConnection(1, 2, 2)
                )
            )
        }
    }

    fun testFun3() {
        viewModelScope.launch(Dispatchers.IO) {
            handleNewMessage(
                MessageDto(
                    listOf(com.ojhdtapp.messagedto.message_content.PlainText("Goodbye at ${System.currentTimeMillis()}")),
                    com.ojhdtapp.messagedto.Profile("Steven", null),
                    com.ojhdtapp.messagedto.Profile("课程群", null),
                    System.currentTimeMillis(),
                    com.ojhdtapp.messagedto.PluginConnection(1, 3, 3)
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
