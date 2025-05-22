package com.ojhdtapp.parabox.domain.built_in.td

import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.domain.model.Extension
import org.drinkless.tdlib.TdApi

object Td {
    val extension = Extension.Success.BuiltIn(
        name = "TDLib",
        icon = null,
        des = "跨平台、功能齐全的Telegram客户端",
        key = "td",
        singleton = true,
        initHandler = TdInitHandler()
    )
}