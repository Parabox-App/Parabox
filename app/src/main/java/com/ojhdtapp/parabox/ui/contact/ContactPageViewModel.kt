package com.ojhdtapp.parabox.ui.contact

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getContact: GetContact,
    val getChat: GetChat,
) : BaseViewModel<ContactPageState, ContactPageEvent, ContactPageEffect>() {
    override fun initialState(): ContactPageState {
        return ContactPageState()
    }

    override suspend fun handleEvent(event: ContactPageEvent, state: ContactPageState): ContactPageState {
        return when(event){
            is ContactPageEvent.LoadContactDetail -> {
                    if (event.contactWithExtensionInfo == null) {
                        state.copy(
                            contactDetail = state.contactDetail.copy(
                                shouldDisplay = false
                            )
                        )
                    } else {
                        loadRelativeChatList(event.contactWithExtensionInfo.contact.contactId)
                        state.copy(
                            contactDetail = ContactPageState.ContactDetail(
                                shouldDisplay = true,
                                contactWithExtensionInfo = event.contactWithExtensionInfo
                            )
                        )
                }
            }

//            is ContactPageEvent.UpdateContactDetailDisplay -> {
//                state.copy(
//                    contactDetail = state.contactDetail.copy(
//                        shouldDisplay = event.shouldDisplay
//                    )
//                )
//            }
            is ContactPageEvent.ToggleFriendOnly -> {
                state.copy(
                    friendOnly = !state.friendOnly
                )
            }

            is ContactPageEvent.UpdateContactRelativeChatList -> {
                state.copy(
                    contactDetail = state.contactDetail.copy(
                        relativeChatState = ContactPageState.ChatState(
                            chatList = event.chatList,
                            loadState = event.loadState
                        )
                    )
                )
            }
        }
    }

    private suspend fun loadRelativeChatList(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            getChat.containsContact(contactId).collectLatest {
                if (it is Resource.Success) {
                    sendEvent(ContactPageEvent.UpdateContactRelativeChatList(it.data ?: emptyList(), LoadState.SUCCESS))
                } else if(it is Resource.Error) {
                    sendEvent(ContactPageEvent.UpdateContactRelativeChatList(emptyList(), LoadState.ERROR))
                }
            }
        }

    }

    val contactPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>> =
        getContact.pagingSource(false).cachedIn(viewModelScope)
    val friendPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>> =
        getContact.pagingSource(true).cachedIn(viewModelScope)
}