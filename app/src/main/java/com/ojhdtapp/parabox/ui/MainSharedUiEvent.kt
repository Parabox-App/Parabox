package com.ojhdtapp.parabox.ui

sealed interface MainSharedUiEvent {
    data class ShowSnackBar(val message: String, val label: String? = null, val callback: (() -> Unit)? = null) : MainSharedUiEvent
}