package com.ojhdtapp.parabox.ui.message

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _pluginInstalledState = mutableStateOf(false)
    val pluginInstalledState = _pluginInstalledState

    fun setPluginInstalledState(value: Boolean){
        _pluginInstalledState.value = value
    }
}