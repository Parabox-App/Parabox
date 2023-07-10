package com.ojhdtapp.parabox.ui.menu

sealed class MenuPageEvent {
    object OnFABClicked: MenuPageEvent()
    object OnMenuClick: MenuPageEvent()
    data class OnDrawerItemClicked(val selfClicked: Boolean): MenuPageEvent()
    object OnBarItemClicked: MenuPageEvent()
}

data class MenuPageUiState(
    val messageBadgeNum: Int = 0
)

