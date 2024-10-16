package com.ojhdtapp.parabox.ui.file

sealed class FilePageUiEvent{
    data class ShowSnackBar(val message: String): FilePageUiEvent()
}