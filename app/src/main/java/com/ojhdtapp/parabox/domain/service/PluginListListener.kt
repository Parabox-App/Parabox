package com.ojhdtapp.parabox.domain.service

import com.ojhdtapp.parabox.domain.model.AppModel

interface PluginListListener {
    fun onPluginListChange(pluginList: List<AppModel>)
}