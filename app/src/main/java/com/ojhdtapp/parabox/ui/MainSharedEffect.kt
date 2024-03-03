package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.ui.base.UiEffect

sealed interface MainSharedEffect: UiEffect{
    object PageListScrollBy : MainSharedEffect
}