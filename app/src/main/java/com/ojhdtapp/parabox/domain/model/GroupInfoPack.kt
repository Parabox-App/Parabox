package com.ojhdtapp.parabox.domain.model

import android.net.Uri
import com.ojhdtapp.parabox.ui.message.GroupEditResource

data class GroupInfoPack(
    val contacts: List<Contact>,
    val pluginConnectionsDistinct: List<PluginConnection>
) {
    fun toGroupEditResource(): GroupEditResource {
        return GroupEditResource(name = contacts.fold(mutableListOf()) { acc, contact ->
            acc.add(contact.profile.name)
            acc
        }, avatar = contacts.fold(mutableListOf()) { acc, contact ->
            contact.profile.avatar?.let { acc.add(it) }
            acc
        }, avatarUri = contacts.fold(mutableListOf()) { acc, contact ->
            contact.profile.avatarUri?.let { acc.add(Uri.parse(it)) }
            acc
        }, pluginConnections = pluginConnectionsDistinct)
    }
}
