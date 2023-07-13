package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.ui.base.UiEvent
import com.ojhdtapp.parabox.ui.message.MessagePageEvent

sealed interface MainSharedEvent: UiEvent {
    data class QueryInput(val input: String) : MainSharedEvent
    data class TriggerSearchBar(val isActive: Boolean): MainSharedEvent
}