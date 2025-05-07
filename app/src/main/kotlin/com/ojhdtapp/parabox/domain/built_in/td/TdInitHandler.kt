package com.ojhdtapp.parabox.domain.built_in.td

import com.ojhdtapp.paraboxdevelopmentkit.init.ParaboxInitHandler
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import org.drinkless.tdlib.TdApi

class TdInitHandler: ParaboxInitHandler() {
    override suspend fun getInitAction(
        list: List<ParaboxInitAction>,
        currentActionIndex: Int
    ): List<ParaboxInitAction> {
        return listOf()
    }

    override suspend fun getConfig(): List<ParaboxConfigItem> {
        TODO("Not yet implemented")
    }
}