package com.ojhdtapp.paraboxdevelopmentkit.init

import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

abstract class ParaboxInitHandler(val data: Bundle = Bundle()) {
    abstract suspend fun getInitAction(list: List<ParaboxInitAction>, currentActionIndex: Int): List<ParaboxInitAction>
    abstract suspend fun getConfig(): List<ParaboxConfigItem>
}