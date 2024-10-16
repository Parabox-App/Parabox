package com.ojhdtapp.parabox.domain.built_in.onebot11

import com.ojhdtapp.parabox.domain.model.Extension

object OneBot11  {
    val extension = Extension.Success.BuiltIn(
        name = "OneBot 11",
        icon = null,
        des = "通用聊天机器人应用接口标准（版本11）",
        key = "onebot11",
        initHandler = OneBot11InitHandler()
    )
}
