package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.domain.model.Profile

// Ui 2 VM
sealed class MessagePageEvent {

}

// VM 2 Ui
sealed class MessagePageUiEvent {
    data class ShowSnackBar(val message: String, val label: String? = null, val callback: (() -> Unit)? = null) : MessagePageUiEvent()
    data class UpdateMessageBadge(val value: Int) : MessagePageUiEvent()
}

sealed class EditActionDialogEvent {
    data class ProfileAndTagUpdate(val contactId: Long, val profile: Profile, val tags: List<String>) :
        EditActionDialogEvent()

    data class EnableNotificationStateUpdate(val contactId: Long, val value: Boolean) :
        EditActionDialogEvent()

    data class PinnedStateUpdate(val contactId: Long, val value: Boolean) : EditActionDialogEvent()

    data class ArchivedStateUpdate(val contactId: Long, val value: Boolean): EditActionDialogEvent()
    object DeleteGrouped: EditActionDialogEvent()
}

sealed class DropdownMenuItemEvent{
    data class Pin(val value: Boolean): DropdownMenuItemEvent()
    object Hide: DropdownMenuItemEvent()
    data class MarkAsRead(val value: Boolean): DropdownMenuItemEvent()
    data class Archive(val value: Boolean): DropdownMenuItemEvent()
    object HideArchive: DropdownMenuItemEvent()
    object UnArchiveALl: DropdownMenuItemEvent()
    object NewTag: DropdownMenuItemEvent()
    object Info: DropdownMenuItemEvent()
    object DeleteGrouped: DropdownMenuItemEvent()
}

sealed class MessageClickingEvent{
    object FailRetry : MessageClickingEvent()
    object Recall : MessageClickingEvent()
    object Favorite : MessageClickingEvent()
    object Copy : MessageClickingEvent()
    object Reply : MessageClickingEvent()
    object Download : MessageClickingEvent()
    object Delete : MessageClickingEvent()
}