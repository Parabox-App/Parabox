package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages

class MessagePageState {
}

data class UngroupedContactState(val isLoading: Boolean = true, val data: List<Contact> = emptyList())

data class MessageState(val state: Int = MessageState.NULL, val data: ContactWithMessages? = null){
    companion object{
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
}