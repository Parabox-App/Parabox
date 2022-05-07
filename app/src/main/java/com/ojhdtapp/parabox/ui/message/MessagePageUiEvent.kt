package com.ojhdtapp.parabox.ui.message

sealed class MessagePageUiEvent {
    class OnConnectPlugin(val pkg: String, val cls:String) : MessagePageUiEvent()
}