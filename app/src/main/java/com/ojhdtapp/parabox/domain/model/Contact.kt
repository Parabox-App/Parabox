package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.ContactEntity

data class Contact(
    val profile: Profile,
    val latestMessage: LatestMessage?,
    val connections: List<PluginConnection>,
    val contactId: Long,
    val isHidden : Boolean = false
){
    // Unused
    fun toContactEntity(): ContactEntity{
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            connections = connections,
            isHidden = isHidden
        )
    }
}
