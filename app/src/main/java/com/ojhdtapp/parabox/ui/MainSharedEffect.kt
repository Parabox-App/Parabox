package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.ui.base.UiEffect

sealed interface MainSharedEffect: UiEffect{
    object PageListScrollBy : MainSharedEffect
    data class LoadMessage(val chat: Chat, val scrollToMessageId: Long? = null) : MainSharedEffect
    data class MenuNavigate(val target: MenuNavigateTarget) : MainSharedEffect
}