package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.PlainText
import com.ojhdtapp.parabox.domain.use_case.GetUngroupedContactList
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    private val handleNewMessage: HandleNewMessage,
    getUngroupedContactList: GetUngroupedContactList
) : ViewModel() {
    init {
        // Update Ungrouped Contacts
        getUngroupedContactList().onEach {
            Log.d("parabox", "contactList:${it}")
            when (it) {
                is Resource.Loading -> {
                    setUngroupedContactState(
                        ungroupedContactState.value.copy(
                            isLoading = true,
                        )
                    )
                }
                is Resource.Error -> {
                    setUngroupedContactState(ungroupedContactState.value.copy(isLoading = false))
                    _uiEventFlow.tryEmit(MessagePageUiEvent.ShowSnackBar(it.message!!))
                }
                is Resource.Success -> {
                    setUngroupedContactState(
                        ungroupedContactState.value.copy(
                            isLoading = false,
                            data = it.data!!
                        )
                    )
                    setMessageBadge(it.data.sumOf { contact ->
                        contact.latestMessage?.unreadMessagesNum ?: 0
                    })
                }
            }
        }.catch {

        }.launchIn(viewModelScope)
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

    // Ungrouped Contact
    private val _ungroupedContactState =
        mutableStateOf<UngroupedContactState>(UngroupedContactState())
    val ungroupedContactState: State<UngroupedContactState> = _ungroupedContactState
    fun setUngroupedContactState(value: UngroupedContactState) {
        _ungroupedContactState.value = value
    }

    private val _searchText = mutableStateOf<String>("")
    val searchText: State<String> = _searchText

    fun setSearchText(value: String) {
        _searchText.value = value
    }

    private val _messageBadge = mutableStateOf<Int>(
        ungroupedContactState.value.data.sumOf {
            it.latestMessage?.unreadMessagesNum ?: 0
        }
    )
    val messageBadge: State<Int> = _messageBadge
    fun setMessageBadge(value: Int) {
        _messageBadge.value = value
    }

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