package com.ojhdtapp.parabox.ui

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
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
                return state.copy(
                    search = state.search.copy(
                        query = event.input
                    )
                )
            }

            is MainSharedEvent.SearchConfirm -> {
                realSearch(event.input)
                return state.copy(
                    search = MainSharedState.Search(
                        query = event.input,
                        isActive = true,
                        showRecent = false,
                        message = MainSharedState.Search.MessageSearch(),
                        contact = MainSharedState.Search.ContactSearch(),
                        chat = MainSharedState.Search.ChatSearch()
                    )
                )
            }

            is MainSharedEvent.TriggerSearchBar -> {
                if (event.isActive) {
                    sendEvent(MainSharedEvent.GetRecentQuery)
                }
                return state.copy(
                    search = state.search.copy(
                        showRecent = true,
                        isActive = event.isActive
                    )
                )
            }

            is MainSharedEvent.GetRecentQuery -> {
                coroutineScope {
                    launch {
                        query.recentQuery().collectLatest {
                            when (it) {
                                is Resource.Success -> {
                                    sendEvent(MainSharedEvent.GetRecentQueryDone(it.data!!, true))
                                }

                                is Resource.Error -> {
                                    sendEvent(MainSharedEvent.GetRecentQueryDone(emptyList(), false))
                                }

                                else -> {}
                            }
                        }
                    }
                }
                return state.copy(
                    search = state.search.copy(
                        recentQueryState = LoadState.LOADING
                    )
                )
            }

            is MainSharedEvent.GetRecentQueryDone -> {
                return state.copy(
                    search = state.search.copy(
                        recentQueryState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                        recentQuery = if (event.isSuccess) event.res else emptyList()
                    )
                )
            }

            is MainSharedEvent.DeleteRecentQuery -> {
                return state.copy(
                    search = state.search.copy(
                        recentQuery = state.search.recentQuery.toMutableList().apply {
                            removeAll { it.id == event.id }
                        }
                    )
                )
            }

            is MainSharedEvent.MessageSearchDone -> {
                return state.copy(
                    search = state.search.copy(
                        message = MainSharedState.Search.MessageSearch(
                            loadState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                            result = event.res
                        )
                    )
                )
            }

            is MainSharedEvent.ContactSearchDone -> {
                return state.copy(
                    search = state.search.copy(
                        contact = MainSharedState.Search.ContactSearch(
                            loadState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                            result = event.res
                        )
                    )
                )
            }

            is MainSharedEvent.ChatSearchDone -> {
                return state.copy(
                    search = state.search.copy(
                        chat = MainSharedState.Search.ChatSearch(
                            loadState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                            result = event.res
                        )
                    )
                )
            }
        }
    }

    private suspend fun realSearch(input: String) {
        coroutineScope {
            launch {
                query.message(input).collectLatest {
                    when (it) {
                        is Resource.Success -> {
                            sendEvent(
                                MainSharedEvent.MessageSearchDone(
                                    res = it.data!!,
                                    isSuccess = true
                                )
                            )
                        }

                        is Resource.Error -> {
                            sendEvent(
                                MainSharedEvent.MessageSearchDone(
                                    res = it.data ?: emptyList(), isSuccess = false
                                )
                            )
                        }

                        else -> {

                        }
                    }
                }
            }
            launch {
                query.contact(input).collectLatest {
                    when (it) {
                        is Resource.Success -> {
                            sendEvent(
                                MainSharedEvent.ContactSearchDone(
                                    res = it.data!!,
                                    isSuccess = true
                                )
                            )
                        }

                        is Resource.Error -> {
                            sendEvent(
                                MainSharedEvent.ContactSearchDone(
                                    res = it.data ?: emptyList(), isSuccess = false
                                )
                            )
                        }

                        else -> {

                        }
                    }
                }
            }
            launch {
                query.chat(input).collectLatest {
                    when (it) {
                        is Resource.Success -> {
                            sendEvent(
                                MainSharedEvent.ChatSearchDone(
                                    res = it.data!!,
                                    isSuccess = true
                                )
                            )
                        }

                        is Resource.Error -> {
                            sendEvent(
                                MainSharedEvent.ChatSearchDone(
                                    res = it.data ?: emptyList(), isSuccess = false
                                )
                            )
                        }

                        else -> {

                        }
                    }
                }
            }
        }

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
