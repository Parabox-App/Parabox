package com.ojhdtapp.parabox.domain.built_in.td

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction

class TdInitHandler: ParaboxInitHandler() {
    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        TODO("Not yet implemented")
    }

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        TODO("Not yet implemented")
    }
}