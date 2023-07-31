package com.ojhdtapp.parabox.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.domain.use_case.Query
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import com.ojhdtapp.parabox.ui.base.UiEffect

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
            is MainSharedEvent.UpdateDataStore -> {
                return state.copy(
                    datastore = state.datastore.copy(
                        messageBadgeNum = context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0),
                        localName = context.getDataStoreValue(DataStoreKeys.USER_NAME, "User"),
                        localAvatarUri = context.getDataStoreValue(DataStoreKeys.USER_AVATAR, "")
                            .takeIf { it.isNotBlank() }
                            ?.let { Uri.parse(it) }
                            ?: Uri.EMPTY,
                    )
                )
            }

            is MainSharedEvent.QueryInput -> {
                realSearchJob?.cancel()
                realSearchJob = null
                if (event.input.isNotBlank()) {
                    realSearchJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(1000)
                        realSearch(event.input, true)
                    }
                }
                if (event.input.isEmpty()) {
                    viewModelScope.launch {
                        delay(200)
                        getRecentSearch()
                    }
                }

                return state.copy(
                    search = state.search.copy(
                        query = event.input,
                        message = state.search.message.copy(
                            loadState = LoadState.LOADING
                        ),
                        chat = state.search.chat.copy(
                            loadState = LoadState.LOADING
                        ),
                        contact = state.search.contact.copy(
                            loadState = LoadState.LOADING
                        )
                    )
                )
            }

            is MainSharedEvent.SearchConfirm -> {
                realSearchJob?.cancel()
                realSearchJob = null
                realSearchJob = viewModelScope.launch(Dispatchers.IO) {
                    query.submitRecentQuery(event.input)
                    realSearch(event.input, false)
                }
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
                    viewModelScope.launch {
                        delay(200)
                        sendEvent(MainSharedEvent.GetRecentQuery)
                        getRecentQuery()
                        getRecentSearch()
                    }
                }
                Log.d("parabox", "trigger search bar:${event.isActive}")
                return state.copy(
                    showNavigationBar = !event.isActive,
                    search = state.search.copy(
                        showRecent = true,
                        isActive = event.isActive,
                        recentQueryState = if (event.isActive) LoadState.LOADING else state.search.recentQueryState,
                    )
                )
            }

            is MainSharedEvent.GetRecentQuery -> {
                Log.d("parabox", "recent query loading")
                return state.copy(
                    search = state.search.copy(
                        recentQueryState = LoadState.LOADING
                    )
                )
            }

            is MainSharedEvent.GetRecentQueryDone -> {
                Log.d("parabox", "recent query success")
                return state.copy(
                    search = state.search.copy(
                        recentQueryState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                        recentQuery = if (event.isSuccess) event.res else emptyList()
                    )
                )
            }

            is MainSharedEvent.DeleteRecentQuery -> {
                coroutineScope {
                    launch(Dispatchers.IO) {
                        query.deleteRecentQuery(event.id)
                    }
                }
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

            is MainSharedEvent.OpenDrawer -> {
                return state.copy(
                    openDrawer = MainSharedState.OpenDrawer(
                        open = event.open,
                        snap = event.snap
                    )
                )
            }

            is MainSharedEvent.OpenBottomSheet -> {
                return state.copy(
                    openBottomSheet = MainSharedState.OpenBottomSheet(
                        open = event.open,
                        snap = event.snap
                    )
                )
            }

            is MainSharedEvent.SearchAvatarClicked -> {
                return state.copy(
                    openMainDialog = !state.openMainDialog
                )
            }

            is MainSharedEvent.ShowNavigationBar -> {
                return state.copy(
                    showNavigationBar = event.show
                )
            }
        }
    }

    private suspend fun getRecentQuery() {
        Log.d("parabox", "getting recent query")
        coroutineScope {
            launch(Dispatchers.IO) {
                query.recentQuery().collectLatest {
                    Log.d("parabox", "recent query res coming:${it}")
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
    }

    private suspend fun getRecentSearch() {
        coroutineScope {
            launch(Dispatchers.IO) {
                query.recentMessage().collectLatest {
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
            launch(Dispatchers.IO) {
                query.recentContact().collectLatest {
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
            launch(Dispatchers.IO) {
                query.recentChat().collectLatest {
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

    private var realSearchJob: Job? = null

    private suspend fun realSearch(input: String, withLimit: Boolean) {
        coroutineScope {
            launch(Dispatchers.IO) {
                query.message(input, withLimit).collectLatest {
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
            launch(Dispatchers.IO) {
                query.contact(input, withLimit).collectLatest {
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
            launch(Dispatchers.IO) {
                query.chat(input, withLimit).collectLatest {
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
        sendEvent(MainSharedEvent.UpdateDataStore)
    }
}
