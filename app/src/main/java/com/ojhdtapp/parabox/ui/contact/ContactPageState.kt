package com.ojhdtapp.parabox.ui.contact

import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.ui.base.UiState

data class ContactPageState(
    val friendOnly: Boolean = true,
    val contactDetail: ContactDetail = ContactDetail()
) : UiState {
    data class ContactDetail(
        val shouldDisplay: Boolean? = null,
        val contactWithExtensionInfo: ContactWithExtensionInfo? = null,
        val relativeChatState: ChatState = ChatState()
    )

    data class ChatState(
        val chatList: List<Chat> = emptyList(),
        val loadState: LoadState = LoadState.LOADING
    )
}
