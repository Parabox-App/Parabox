package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class ExtensionInitActionWrapper(
    val key: String? = null,
    val name: String? = null,
    val extensionInfo: ExtensionInfo? = null,
    val actionList: List<ParaboxInitAction> = emptyList(),
    val currentIndex: Int = -1,
)
