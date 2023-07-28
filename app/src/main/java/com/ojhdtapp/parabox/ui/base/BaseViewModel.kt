package com.ojhdtapp.parabox.ui.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel< S : UiState, E : UiEvent,F : UiEffect>  : ViewModel() {
    private val initialState: S by lazy { initialState() }
    protected abstract fun initialState(): S

    private val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(initialState) }
    val uiState: StateFlow<S> by lazy { _uiState }

    private fun sendState(newState: S.() -> S) {
        Log.d("parabox", "new state: ${uiState.value.newState()}")
        _uiState.value = uiState.value.newState()
    }

    private val _uiEvent: MutableSharedFlow<E> = MutableSharedFlow()

    init {
        subscribeEvents()
    }

    private fun subscribeEvents() {
        viewModelScope.launch {
            _uiEvent.collect {
                reduceEvent(_uiState.value, it)
            }
        }
    }

    fun sendEvent(event: E) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    private fun reduceEvent(state: S, event: E) {
        viewModelScope.launch {
            handleEvent(event, state)?.let { newState -> sendState { newState } }
        }
    }

    protected abstract suspend fun handleEvent(event: E, state: S): S?

    private val _uiEffect: MutableSharedFlow<F> = MutableSharedFlow()
    val uiEffect: Flow<F> = _uiEffect

    protected fun sendEffect(effect: F) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}

interface UiState

interface UiEvent

interface UiEffect