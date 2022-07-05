package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock

class MessagePageState {
}

data class ContactState(val isLoading: Boolean = true, val data: List<Contact> = emptyList())

data class MessageState(val state: Int = MessageState.NULL, val profile: Profile? = null, val data: Map<Long, List<ChatBlock>>? = null, val message: String? = null){
    companion object{
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
}