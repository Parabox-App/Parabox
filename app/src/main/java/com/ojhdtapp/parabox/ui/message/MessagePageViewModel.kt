package com.ojhdtapp.parabox.ui.message

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ojhdtapp.parabox.domain.plugin.Conn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(

) : ViewModel() {
    lateinit var pluginConnection: Conn
    fun onEvent(event: MessagePageUiEvent){
        when(event){
            is MessagePageUiEvent.OnConnectPlugin -> {
                pluginConnection.connect(event.pkg, event.cls)
            }
            else -> {}
        }
    }

    private val _pluginInstalledState = mutableStateOf(false)
    val pluginInstalledState = _pluginInstalledState

    fun setPluginInstalledState(value: Boolean){
        _pluginInstalledState.value = value
    }
}