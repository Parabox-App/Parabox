package com.ojhdtapp.parabox.domain.service.extension

import android.content.pm.PackageInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

data class ExtensionInitActionWrapper(
    val packageInfo: PackageInfo? = null,
    val actionList: List<ParaboxInitAction> = emptyList(),
    val currentIndex: Int = -1,
)
