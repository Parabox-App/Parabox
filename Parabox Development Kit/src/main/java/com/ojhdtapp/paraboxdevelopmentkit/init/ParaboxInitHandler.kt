package com.ojhdtapp.paraboxdevelopmentkit.init

import android.content.Context
import android.os.Bundle
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import org.json.JSONObject

abstract class ParaboxInitHandler(val data: JSONObject = JSONObject()) {
    var context: Context? = null
        private set
    fun attachContext(context: Context) {
        this.context = context
        onInit()
    }
    fun finish() {
        context = null
        onDestroy()
    }
    abstract fun onInit()
    abstract suspend fun getInitAction(list: List<ParaboxInitAction>, currentActionIndex: Int): List<ParaboxInitAction>
    abstract suspend fun getConfig(): List<ParaboxConfigItem>
    abstract fun onDestroy()
}