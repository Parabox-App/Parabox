package com.ojhdtapp.parabox.ui.message

import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.base.UiEffect

sealed interface MessagePageEffect : UiEffect{
    data class ShowSnackBar(val message: String, val label: String? = null, val callback: (() -> Unit)? = null) :
        MessagePageEffect
    data class ImagePreviewerOpenTransform(val index: Int) : MessagePageEffect
}