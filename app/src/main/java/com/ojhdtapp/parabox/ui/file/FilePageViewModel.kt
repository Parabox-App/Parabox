package com.ojhdtapp.parabox.ui.file

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FilePageViewModel : ViewModel() {
    private val _uiEventFlow = MutableSharedFlow<FilePageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _searchText = mutableStateOf<String>("")
    val searchText : State<String> = _searchText

    fun setSearchText(value : String){
        _searchText.value = value
    }
}