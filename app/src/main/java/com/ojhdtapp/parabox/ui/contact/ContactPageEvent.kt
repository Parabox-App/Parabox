package com.ojhdtapp.parabox.ui.contact

import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo
import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface ContactPageEvent : UiEvent {
    data class LoadContactDetail(val contactWithExtensionInfo: ContactWithExtensionInfo) : ContactPageEvent
    data class UpdateContactDetailDisplay(val shouldDisplay: Boolean) : ContactPageEvent
    data object TriggerFriendOnly : ContactPageEvent
}