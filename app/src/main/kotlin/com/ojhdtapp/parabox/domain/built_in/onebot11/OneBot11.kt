package com.ojhdtapp.parabox.domain.built_in.onebot11

import com.ojhdtapp.parabox.domain.built_in.BuiltInExtensionUtil
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint

object OneBot11 {
    const val KEY = "onebot11"

    val provider = OneBot11ExtensionProvider()

    val extension = Extension.Success.BuiltIn(
        name = provider.descriptor.name,
        icon = null,
        des = provider.descriptor.description,
        key = KEY,
        endpoint = ExtensionEndpoint.InProcess(
            serverName = BuiltInExtensionUtil.IN_PROCESS_SERVER_NAME,
            extensionKey = KEY,
        ),
    )
}
