package com.ojhdtapp.paraboxdevelopmentkit.v2

sealed interface ParaboxConnectionStatus {
    data object Pending : ParaboxConnectionStatus
    data object Initializing : ParaboxConnectionStatus
    data object Active : ParaboxConnectionStatus
    data class Reconnecting(val attempt: Int = 1, val message: String = "") : ParaboxConnectionStatus
    data class Error(val message: String) : ParaboxConnectionStatus
}
