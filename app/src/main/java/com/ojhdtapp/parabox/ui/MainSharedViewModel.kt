package com.ojhdtapp.parabox.ui

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.use_case.Query
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import com.ojhdtapp.parabox.ui.base.UiEffect
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
    val query: Query,
) : BaseViewModel<MainSharedState, MainSharedEvent, UiEffect>() {

    override fun initialState(): MainSharedState {
        return MainSharedState()
    }

    override suspend fun handleEvent(
        event: MainSharedEvent,
        state: MainSharedState
    ): MainSharedState? {
        when (event) {
            is MainSharedEvent.QueryInput -> {
                searchJob?.cancel()
                searchJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(800L)
                    realSearch()
                }
                return state.copy(
                    search = state.search.copy(
                        query = event.input
                    )
                )
            }

            is MainSharedEvent.TriggerSearchBar -> {
                return state.copy(
                    search = state.search.copy(
                        isActive = event.isActive
                    )
                )
            }
        }
    }

    private var searchJob: Job? = null
    private suspend fun realSearch() {

    }

    init {
        // update badge num
        viewModelScope.launch(Dispatchers.IO) {
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

    // MenuPage State
    private val _menuPageUiState = MutableStateFlow(MenuPageUiState())
    val menuPageUiState = _menuPageUiState.asStateFlow()

}
