package com.ojhdtapp.parabox.ui.contact

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.domain.model.Contact
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
) : BaseViewModel<ContactPageState, ContactPageEvent, ContactPageEffect>() {
    override fun initialState(): ContactPageState {
        return ContactPageState()
    }

    override suspend fun handleEvent(event: ContactPageEvent, state: ContactPageState): ContactPageState? {
        return when(event){
            else -> state
        }
    }

    val contactPagingDataFlow: Flow<PagingData<Contact>> =
        getContact.pagingSource().cachedIn(viewModelScope)
}