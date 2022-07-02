package com.ojhdtapp.parabox.ui.util

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph

@RootNavGraph(start = true)
@NavGraph
annotation class MessageNavGraph(
    val start: Boolean = false
)

@RootNavGraph(start = false)
@NavGraph
annotation class FileNavGraph(
    val start: Boolean = false
)

@RootNavGraph(start = false)
@NavGraph
annotation class SettingNavGraph(
    val start: Boolean = false
)