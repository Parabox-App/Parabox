package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.ExtensionInfo

data class ContactWithExtensionInfo(
    val contact: Contact,
    val extensionInfo: ExtensionInfo
)
