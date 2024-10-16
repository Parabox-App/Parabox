package com.ojhdtapp.parabox.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class FixedInsets(
    val statusBarHeight: Dp = 0.dp,
    val navigationBarHeight: Dp = 0.dp
)

val LocalFixedInsets = compositionLocalOf<FixedInsets> { error("no FixedInsets provided!") }