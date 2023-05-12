package com.ojhdtapp.parabox.ui.common

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph

@NavGraph
annotation class MenuNavGraph(
    val start: Boolean = false
)

@RootNavGraph(start = false)
@NavGraph
annotation class GuideNavGraph(
    val start: Boolean = false
)

@MenuNavGraph(start = true)
@NavGraph
annotation class MessageNavGraph(
    val start: Boolean = false
)

@MenuNavGraph(start = false)
@NavGraph
annotation class FileNavGraph(
    val start: Boolean = false
)