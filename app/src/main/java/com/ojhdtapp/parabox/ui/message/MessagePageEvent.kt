package com.ojhdtapp.parabox.ui.message

// Ui 2 VM
sealed class MessagePageEvent {

}

// VM 2 Ui
sealed class MessagePageUiEvent{
    data class ShowSnackBar(val message:String) : MessagePageUiEvent()
}