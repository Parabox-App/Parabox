package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val avatar: ParaboxResourceInfo,
    val pkg: String,
    val uid: String,
    val connectionId: Long,
    val isFriend: Boolean,
    val contactId: Long,
) : Parcelable {
    var name: String = ""
        get() = field.ifBlank { uid }

    constructor(
        name: String?,
        avatar: ParaboxResourceInfo,
        pkg: String,
        uid: String,
        connectionId: Long,
        isFriend: Boolean,
        contactId: Long
    ) : this(avatar, pkg, uid, connectionId, isFriend, contactId) {
        this.name = name ?: ""
    }

    fun platformEqual(other: Contact?): Boolean {
        return other != null && pkg == other.pkg && uid == other.uid
    }
}