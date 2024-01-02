package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

data class Contact(
    val avatar: ParaboxResourceInfo,
    val pkg: String,
    val uid: String,
    val contactId: Long,
) {
    var name: String = ""
        get() = field.ifBlank { uid }

    constructor(
        name: String?,
        avatar: ParaboxResourceInfo,
        pkg: String,
        uid: String,
        contactId: Long
    ) : this(avatar, pkg, uid, contactId) {
        this.name = name ?: ""
    }

    fun platformEqual(other: Contact?): Boolean {
        return other != null && pkg == other.pkg && uid == other.uid
    }
}