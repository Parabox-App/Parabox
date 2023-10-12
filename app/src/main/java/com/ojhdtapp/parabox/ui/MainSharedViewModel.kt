package com.ojhdtapp.parabox.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.filter.MessageFilter
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
                    datastore = event.value
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
                        showRecent = true,
                        message = state.search.message.copy(
                            loadState = LoadState.LOADING,
                            result = emptyList(),
                            filterResult = emptyList(),
                        ),
                        chat = state.search.chat.copy(
                            loadState = LoadState.LOADING,
                            result = emptyList(),
                            filterResult = emptyList(),
                        ),
                        contact = MainSharedState.Search.ContactSearch()
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
                        showRecent = false
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
                    search =  MainSharedState.Search(
                        isActive = event.isActive
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
                            result = event.res,
                            filterResult = event.res
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
                            result = event.res,
                            filterResult = event.res
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

            is MainSharedEvent.UpdateSearchDoneChatFilter -> {
                val newEnabledFilterList = if (state.search.chat.enabledFilterList.contains(event.filter)) {
                    state.search.chat.enabledFilterList.toMutableList().apply {
                        remove(event.filter)
                    }
                } else {
                    state.search.chat.enabledFilterList.toMutableList().apply {
                        add(event.filter)
                    }
                }
                return state.copy(
                    search = state.search.copy(
                        chat = state.search.chat.copy(
                            enabledFilterList = newEnabledFilterList,
                            filterResult = state.search.chat.result.filter { chat ->
                                newEnabledFilterList.all { it.check(chat) }
                            }
                        )
                    )
                )
            }

            is MainSharedEvent.UpdateSearchDoneMessageFilter -> {
                val newFilterList = state.search.message.filterList.toMutableList().apply {
                    when (event.filter) {
                        is MessageFilter.SenderFilter -> {
                            set(0, event.filter)
                        }

                        is MessageFilter.ChatFilter -> {
                            set(1, event.filter)
                        }

                        is MessageFilter.DateFilter -> {
                            set(2, event.filter)
                        }
                    }
                }
                return state.copy(
                    search = state.search.copy(
                        message = state.search.message.copy(
                            filterList = newFilterList,
                            filterResult = state.search.message.result.filter { queryMessage ->
                                newFilterList.all { it.check(queryMessage.message) }
                            }
                        )
                    )
                )
            }

            is MainSharedEvent.PickChat -> {
                onPickChatDone = event.onDone
                viewModelScope.launch(Dispatchers.IO) {
                    chatPickerQuery("")
                }
                return state.copy(
                    chatPicker = MainSharedState.ChatPicker(
                        showDialog = true,
                        loadState = LoadState.LOADING,
                        query = "",
                        result = emptyList(),
                    )
                )
            }

            is MainSharedEvent.PickChatQueryInput -> {
                chatPickerQueryJob?.cancel()
                chatPickerQueryJob = null
                chatPickerQueryJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(1000)
                    chatPickerQuery(event.input)
                }
                return state.copy(
                    chatPicker = state.chatPicker.copy(
                        query = event.input,
                        loadState = LoadState.LOADING,
                    )
                )
            }

            is MainSharedEvent.GetPickChatDone -> {
                return state.copy(
                    chatPicker = state.chatPicker.copy(
                        loadState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                        result = event.res
                    )
                )
            }

            is MainSharedEvent.PickChatDone -> {
                viewModelScope.launch(Dispatchers.IO) {
                    onPickChatDone?.invoke(event.res)
                }
                return state.copy(
                    chatPicker = MainSharedState.ChatPicker()
                )
            }

            is MainSharedEvent.PickContact -> {
                viewModelScope.launch(Dispatchers.IO) {
                    contactPickerQuery("")
                }
                onPickContactDone = event.onDone
                return state.copy(
                    contactPicker = MainSharedState.ContactPicker(
                        showDialog = true,
                        loadState = LoadState.LOADING,
                        query = "",
                        result = emptyList(),
                    )
                )
            }

            is MainSharedEvent.PickContactQueryInput -> {
                contactPickerJob?.cancel()
                contactPickerJob = null
                if (event.input.isNotBlank()) {
                    contactPickerJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(1000)
                        contactPickerQuery(event.input)
                    }
                }
                return state.copy(
                    contactPicker = state.contactPicker.copy(
                        query = event.input,
                        loadState = LoadState.LOADING,
                    )
                )
            }

            is MainSharedEvent.GetPickContactDone -> {
                return state.copy(
                    contactPicker = state.contactPicker.copy(
                        loadState = if (event.isSuccess) LoadState.SUCCESS else LoadState.ERROR,
                        result = event.res
                    )
                )
            }

            is MainSharedEvent.PickContactDone -> {
                viewModelScope.launch(Dispatchers.IO) {
                    onPickContactDone?.invoke(event.res)
                }
                return state.copy(
                    contactPicker = MainSharedState.ContactPicker()
                )
            }

            is MainSharedEvent.PickDateRange -> {
                onPickDateRangeDone = event.onDone
                return state.copy(
                    openDateRangePicker = true
                )
            }

            is MainSharedEvent.PickDateRangeDone -> {
                viewModelScope.launch(Dispatchers.IO) {
                    onPickDateRangeDone?.invoke(event.res)
                }
                return state.copy(
                    openDateRangePicker = false
                )
            }
        }
    }

    private suspend fun getRecentQuery() {
        coroutineScope {
            launch(Dispatchers.IO) {
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

    private var chatPickerQueryJob: Job? = null
    private var contactPickerJob: Job? = null
    private var onPickChatDone: ((Chat?) -> Unit)? = null
    private var onPickContactDone: ((Contact?) -> Unit)? = null
    private var onPickDateRangeDone: ((Pair<Long, Long>?) -> Unit)? = null
    private suspend fun chatPickerQuery(input: String) {
        coroutineScope {
            launch(Dispatchers.IO) {
                Log.d("parabox", "chat picker query: $input")
                query.chat(input, false).collectLatest {
                    Log.d("parabox", "chat picker res coming:${it}")
                    when (it) {
                        is Resource.Success -> {
                            sendEvent(
                                MainSharedEvent.GetPickChatDone(
                                    res = it.data!!,
                                    isSuccess = true
                                )
                            )
                        }

                        is Resource.Error -> {
                            sendEvent(
                                MainSharedEvent.GetPickChatDone(
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

    private suspend fun contactPickerQuery(input: String) {
        coroutineScope {
            launch(Dispatchers.IO) {
                query.contact(input, false).collectLatest {
                    when (it) {
                        is Resource.Success -> {
                            sendEvent(
                                MainSharedEvent.GetPickContactDone(
                                    res = it.data!!,
                                    isSuccess = true
                                )
                            )
                        }

                        is Resource.Error -> {
                            sendEvent(
                                MainSharedEvent.GetPickContactDone(
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
        state.datastore.copy(
            messageBadgeNum = context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0),
            localName = context.getDataStoreValue(DataStoreKeys.USER_NAME, "User"),
            localAvatarUri = context.getDataStoreValue(DataStoreKeys.USER_AVATAR, "")
                .takeIf { it.isNotBlank() }
                ?.let { Uri.parse(it) }
                ?: Uri.EMPTY,
        )
        sendEvent(MainSharedEvent.UpdateDataStore)
    }
}
