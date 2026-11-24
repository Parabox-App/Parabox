package com.ojhdtapp.parabox.ui.common

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.staticCompositionLocalOf

internal fun ComponentActivity.enableTransparentNavigationBar(
    statusBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
) {
    enableEdgeToEdge(statusBarStyle, navigationBarStyle)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Edge-to-edge enables a system contrast scrim for three-button navigation.
        window.isNavigationBarContrastEnforced = false
    }
}

class SystemUiController(val activity: ComponentActivity) {
    fun setStatusBarColor(isLight: Boolean) {
        activity.enableTransparentNavigationBar(
            statusBarStyle = if (isLight) SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
            ) else SystemBarStyle.dark(Color.TRANSPARENT),
        )
    }

    fun reset() {
        activity.enableTransparentNavigationBar()
    }
}

val LocalSystemUiController = staticCompositionLocalOf<SystemUiController> {
    error("No SystemUiController provided!")
}