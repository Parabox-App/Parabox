package com.ojhdtapp.parabox.ui.message

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getChat: GetChat,
    val getContact: GetContact,
) : ViewModel() {
    private var _pageStateFlow = MutableStateFlow(MessagePageState())
    val pageStateFlow get() = _pageStateFlow.asStateFlow()

    // Datastore
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val enableSwipeToDismiss =
                context.getDataStoreValue(DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS, false)
            _pageStateFlow.value = pageStateFlow.value.copy(
                datastore = MessagePageState.DataStore(
                    enableSwipeToDismiss = enableSwipeToDismiss
                )
            )
        }
    }

    private val chatLatestMessageSenderMap = mutableMapOf<Long, Resource<Contact>>()

    fun getLatestMessageSenderWithCache(senderId: Long?): Flow<Resource<Contact>> {
        return flow {
            if (senderId == null) {
                emit(Resource.Error("no sender"))
            } else {
                if (chatLatestMessageSenderMap[senderId] != null) {
                    emit(chatLatestMessageSenderMap[senderId]!!)
                } else {
                    emitAll(
                        getContact.byId(senderId).onEach {
                            if (it is Resource.Success) {
                                chatLatestMessageSenderMap[senderId] = it
                            }
                        }
                    )
                }
            }
        }
    }

    fun getChatPagingDataFlow(): Flow<PagingData<ChatWithLatestMessage>> {
        return getChat(pageStateFlow.value.enabledGetChatFilterList).cachedIn(viewModelScope)
    }

    fun addOrRemoveEnabledGetChatFilter(filter: GetChatFilter) {
        viewModelScope.launch {
            val newList = if (pageStateFlow.value.enabledGetChatFilterList.contains(filter)) {
                pageStateFlow.value.enabledGetChatFilterList.toMutableList().apply {
                    remove(filter)
                }
            } else {
                pageStateFlow.value.enabledGetChatFilterList.toMutableList().apply {
                    add(filter)
                }
            }.ifEmpty { listOf(GetChatFilter.Normal) }
            _pageStateFlow.value = pageStateFlow.value.copy(
                enabledGetChatFilterList = newList
            )
        }
    }

    fun addOrRemoveSelectedGetChatFilter(filter: GetChatFilter) {
        if(filter is GetChatFilter.Normal) return
        viewModelScope.launch {
            val newList = if (pageStateFlow.value.selectedGetChatFilterList.contains(filter)) {
                pageStateFlow.value.selectedGetChatFilterList.toMutableList().apply {
                    remove(filter)
                }
            } else {
                pageStateFlow.value.selectedGetChatFilterList.toMutableList().apply {
                    add(filter)
                }
            }
            _pageStateFlow.value = pageStateFlow.value.copy(
                selectedGetChatFilterList = newList
            )
        }
    }
}