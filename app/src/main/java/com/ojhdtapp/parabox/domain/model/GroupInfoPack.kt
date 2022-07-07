package com.ojhdtapp.parabox.domain.model

data class GroupInfoPack(
    val contacts: List<Contact>,
    val pluginConnectionsDistinct: List<PluginConnection>
)
