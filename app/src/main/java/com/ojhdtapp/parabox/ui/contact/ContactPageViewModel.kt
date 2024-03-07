package com.ojhdtapp.parabox.ui.contact

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

    override suspend fun handleEvent(event: ContactPageEvent, state: ContactPageState): ContactPageState? {
        return when(event){
            is ContactPageEvent.LoadContactDetail -> {
                state.copy(
                    contactDetail = state.contactDetail.copy(
                        contactWithExtensionInfo = event.contactWithExtensionInfo
                    )
                )
            }

            is ContactPageEvent.UpdateContactDetailDisplay -> {
                state.copy(
                    contactDetail = state.contactDetail.copy(
                        shouldDisplay = event.shouldDisplay
                    )
                )
            }
            is ContactPageEvent.ToggleFriendOnly -> {
                state.copy(
                    friendOnly = !state.friendOnly
                )
            }
        }
    }

    val contactPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>> =
        getContact.pagingSource(false).cachedIn(viewModelScope)
    val friendPagingDataFlow: Flow<PagingData<ContactWithExtensionInfo>> =
        getContact.pagingSource(true).cachedIn(viewModelScope)
}