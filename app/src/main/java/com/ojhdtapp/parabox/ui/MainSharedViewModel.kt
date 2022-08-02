package com.ojhdtapp.parabox.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.use_case.GetMessages
import com.ojhdtapp.parabox.ui.message.MessageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainSharedViewModel @Inject constructor(
    val getMessages: GetMessages,
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

    private var _messagePagingDataFlow = emptyFlow<PagingData<Message>>()

    private val _editingContact = mutableStateOf<Long?>(null)
    val editingContact: State<Long?> = _editingContact
    private var messageJob: Job? = null

    @OptIn(FlowPreview::class)
    fun receiveAndUpdateMessageFromContact(contact: Contact, shouldSelect: Boolean) {
        messageJob?.cancel()
        if (shouldSelect) {
            _editingContact.value = contact.contactId
        }
        messageJob = viewModelScope.launch(Dispatchers.IO) {
            getMessages.pagingFlow(contact = contact)
                .cachedIn(viewModelScope)


//            getMessages(contact = contact).collectLatest {
//                _messageStateFlow.value = when (it) {
//                    is Resource.Loading -> MessageState(
//                        state = MessageState.LOADING,
//                        profile = contact.profile
//                    )
//                    is Resource.Error -> MessageState(
//                        state = MessageState.ERROR,
//                        profile = contact.profile,
//                        message = it.message
//                    )
//                    is Resource.Success -> MessageState(
//                        state = MessageState.SUCCESS,
//                        profile = contact.profile,
//                        data = it.data!!.toTimedMessages()
//                    )
//                }
//            }

        }
    }

    fun cancelMessage() {
        messageJob?.cancel()
        _editingContact.value = null
        _messageStateFlow.value = MessageState()
    }
}