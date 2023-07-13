package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.ui.base.UiEffect

sealed interface MainSharedEffect: UiEffect{
    data class ShowSnackBar(val message: String, val label: String? = null, val callback: (() -> Unit)? = null) : MainSharedEffect
}