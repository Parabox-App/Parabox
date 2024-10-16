package com.ojhdtapp.parabox.ui.menu

sealed class MenuPageEvent {
    object OnFABClicked: MenuPageEvent()
    object OnMenuClick: MenuPageEvent()
    data class OnDrawerItemClicked(val selfClicked: Boolean): MenuPageEvent()
    data class OnBarItemClicked(val selfClicked: Boolean): MenuPageEvent()
}