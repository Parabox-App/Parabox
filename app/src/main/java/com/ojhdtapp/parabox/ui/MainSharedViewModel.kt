package com.ojhdtapp.parabox.ui

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.ui.menu.MenuPageUiState

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainSharedViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {
    init {
        // update badge num
        viewModelScope.launch(Dispatchers.IO){
            context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { settings ->
                    settings[DataStoreKeys.MESSAGE_BADGE_NUM] ?: 0
                }.collectLatest {
                    _menuPageUiState.value = menuPageUiState.value.copy(
                        messageBadgeNum = it
                    )
                }

        }
    }

    private val _uiEventFlow = MutableSharedFlow<MainSharedUiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    // MenuPage State
    private val _menuPageUiState = MutableStateFlow(MenuPageUiState())
    val menuPageUiState = _menuPageUiState.asStateFlow()
}
