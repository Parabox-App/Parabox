package com.ojhdtapp.parabox.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainScreenSharedViewModel : ViewModel() {
    private val _messageBadge = mutableStateOf<Int>(0)
    val messageBadge: State<Int> = _messageBadge
    fun setMessageBadge(value: Int) {
        _messageBadge.value = value
    }
}