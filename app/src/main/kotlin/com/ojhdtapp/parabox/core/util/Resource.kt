package com.ojhdtapp.parabox.core.util

import androidx.compose.runtime.Stable

@Stable
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Success<T>(data: T?) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}

enum class LoadState{
    LOADING, SUCCESS, ERROR
}