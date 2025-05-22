package com.ojhdtapp.parabox.domain.built_in.td.model

enum class AuthorizationState {
    Closed, WaitTdlibParameter, WaitPhoneNumber, WaitCode, WaitPassword, Ready
}