package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class ExtensionInitActionWrapper(
    val key: String,
    val name: String,
    val extensionInfo: Extension.Success,
    val actionList: List<ParaboxInitAction> = emptyList(),
    val currentIndex: Int = -1,
)
