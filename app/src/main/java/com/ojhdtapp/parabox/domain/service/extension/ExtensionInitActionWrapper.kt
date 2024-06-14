package com.ojhdtapp.parabox.domain.service.extension

import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class ExtensionInitActionWrapper(
    val key: String? = null,
    val name: String? = null,
    val connection: Connection? = null,
    val actionList: List<ParaboxInitAction> = emptyList(),
    val currentIndex: Int = -1,
)
