package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

data class Contact(
    val name: String,
    val avatar: ParaboxResourceInfo,
    val pkg: String,
    val uid: String,
    val contactId: Long,
)