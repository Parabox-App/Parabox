package com.ojhdtapp.parabox.ui.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

abstract class BaseViewModel< S : UiState, E : UiEvent,F : UiEffect>  : ViewModel() {
    private val initialState: S by lazy { initialState() }
    protected abstract fun initialState(): S

    private val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(initialState) }
    val uiState: StateFlow<S> by lazy { _uiState }

    private fun sendState(event: E, newState: S.() -> S) {
        Log.d("parabox", "new state: ${uiState.value.newState()}")
        _uiState.value = uiState.value.newState()
        if (event.lock) {
            mutex.unlock()
            Log.d("parabox", "unlock for event:${event}")
        }
    }

    private val _uiEvent: MutableSharedFlow<E> = MutableSharedFlow()

    private val mutex = Mutex()

    init {
        subscribeEvents()
    }

    private fun subscribeEvents() {
        viewModelScope.launch {
            _uiEvent.collect {
                if (it.lock) {
                    mutex.lock()
                    Log.d("parabox", "lock for event:${it}")
                }
                reduceEvent(_uiState.value, it)
            }
        }
    }

    fun sendEvent(event: E) {
        Log.d("parabox", "new event: ${event}")
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    private fun reduceEvent(state: S, event: E) {
        viewModelScope.launch {
            handleEvent(event, state)?.let { newState -> sendState(event) { newState } }
        }
    }

    protected abstract suspend fun handleEvent(event: E, state: S): S?

    private val _uiEffect: MutableSharedFlow<F> = MutableSharedFlow()
    val uiEffect: Flow<F> = _uiEffect

    protected fun sendEffect(effect: F) {
        Log.d("parabox", "new effect: ${effect}")
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}

interface UiState

interface UiEvent {
    val lock: Boolean
        get() = false
}

interface UiEffect