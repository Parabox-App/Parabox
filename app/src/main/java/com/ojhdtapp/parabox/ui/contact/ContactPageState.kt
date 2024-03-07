package com.ojhdtapp.parabox.ui.contact

import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.ui.base.UiState

data class ContactPageState(
    val friendOnly: Boolean = true,
    val contactDetail: ContactDetail = ContactDetail()
) : UiState {
    data class ContactDetail(
        val shouldDisplay: Boolean = false,
        val contactWithExtensionInfo: ContactWithExtensionInfo? = null
    )
}
