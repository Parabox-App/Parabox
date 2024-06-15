package com.ojhdtapp.parabox.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.color.DynamicColors

fun Context.launchPlayStore(pkg: String) {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$pkg")
            )
        )
    } catch (e: ActivityNotFoundException) {
        BrowserUtil.launchURL(
            context = this,
            url = "https://play.google.com/store/apps/details?id=$pkg"
        )
    }
}

fun Context.launchNotificationSetting() {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}

fun Context.launchLocaleSetting() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_LOCALE_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    } else {
        launchSetting()
    }
}
fun Context.launchLocationSetting() {
    val intent = Intent().apply {
        action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
    }
    startActivity(intent)
}

fun Context.launchSetting() {
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

fun Context.getThemeColor(attrRes: Int): Int {
    val dynamicColorContext = DynamicColors.wrapContextIfAvailable(
        this,
        com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight
    )
    val typedValue = dynamicColorContext.obtainStyledAttributes(intArrayOf(attrRes))
    val color = typedValue.getColor(0, 0)
    typedValue.recycle()
    return color
}

fun Context.requestIgnoringBatteryOptimizationSetting() {
    val intent = Intent().apply {
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}