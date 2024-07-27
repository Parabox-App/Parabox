package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.ConnectionInfo

data class ContactWithExtensionInfo(
    val contact: Contact,
    val connectionInfo: ConnectionInfo
)
