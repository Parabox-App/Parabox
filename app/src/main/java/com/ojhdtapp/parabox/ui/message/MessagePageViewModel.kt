package com.ojhdtapp.parabox.ui.message

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(

) : ViewModel() {
    fun onEvent(event: MessagePageEvent){
        when(event){

            else -> {

            }
        }
    }

    private val _uiEventFlow = MutableSharedFlow<MessagePageUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _pluginInstalledState = mutableStateOf(false)
    val pluginInstalledState = _pluginInstalledState

    fun setPluginInstalledState(value: Boolean){
        _pluginInstalledState.value = value
    }

    private val _sendAvailableState = mutableStateOf<Boolean>(false)
    val sendAvailableState : State<Boolean> = _sendAvailableState

    fun setSendAvailableState(value : Boolean){
        _sendAvailableState.value = value
    }

    private val _message = mutableStateOf<String>("Text")
    val message : State<String> = _message

    fun setMessage(value : String){
        _message.value = value
    }
}