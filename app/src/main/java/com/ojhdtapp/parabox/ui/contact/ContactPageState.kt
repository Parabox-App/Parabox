package com.ojhdtapp.parabox.ui.contact

import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.base.UiState

data class ContactPageState(
    val contactDetail: ContactDetail = ContactDetail()
) : UiState {
    data class ContactDetail(
        val contact: Contact? = null
    )
}
