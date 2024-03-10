package com.ojhdtapp.parabox.ui.common

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.staticCompositionLocalOf

class SystemUiController(val activity: ComponentActivity) {
    fun setStatusBarColor(isLight: Boolean) {
        val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
        activity.enableEdgeToEdge(
            statusBarStyle = if (isLight) SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
    }

    fun reset() {
        activity.enableEdgeToEdge()
    }
}

val LocalSystemUiController = staticCompositionLocalOf<SystemUiController> {
    error("No SystemUiController provided!")
}