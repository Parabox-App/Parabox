package com.ojhdtapp.parabox.ui.menu

sealed class MenuPageEvent {
    object OnFABClicked: MenuPageEvent()
    object OnDrawerClose: MenuPageEvent()
    data class OnDrawerItemClicked(val selfClicked: Boolean): MenuPageEvent()
    object onBarItemClicked: MenuPageEvent()
}

data class MenuPageUiState(
    val messageBadgeNum: Int = 0
)

