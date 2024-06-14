package com.ojhdtapp.paraboxdevelopmentkit.init

import android.content.pm.PackageInfo
import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

abstract class ParaboxInitHandler(val data: Bundle = Bundle()) {
    abstract suspend fun getExtensionInitActions(list: List<ParaboxInitAction>, currentActionIndex: Int): List<ParaboxInitAction>
}