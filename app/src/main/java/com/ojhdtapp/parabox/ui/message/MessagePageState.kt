package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Contact

class MessagePageState {
}

data class UngroupedContactState(val isLoading: Boolean = true, val data: List<Contact> = emptyList())