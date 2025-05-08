package com.ojhdtapp.parabox.domain.built_in.td

import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.domain.model.Extension
import org.drinkless.tdlib.TdApi

object Td {
    val extension = Extension.Success.BuiltIn(
        name = "Telegram",
        icon = null,
        des = "Telegram",
        key = "td",
        initHandler = TdInitHandler()
    )
}