package com.ojhdtapp.parabox.domain.built_in.kritor

import com.ojhdtapp.parabox.domain.model.Extension

object Kritor {
    val extension = Extension.Success.BuiltIn(
        name = "Kritor",
        icon = null,
        des = "新时代统一的聊天机器人应用接口标准",
        key = "kritor",
        initHandler = KritorInitHandler()
    )
}