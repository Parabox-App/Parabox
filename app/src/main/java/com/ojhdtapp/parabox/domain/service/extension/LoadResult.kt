package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.model.Extension

sealed class LoadResult {
    class Success(val extension: Extension) : LoadResult()
    object Error : LoadResult()
}