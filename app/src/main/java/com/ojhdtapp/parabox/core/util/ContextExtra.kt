package com.ojhdtapp.parabox.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.ojhdtapp.parabox.R


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

fun Context.launchSetting() {
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

fun Context.getThemeColor(attrRes: Int): Int {
    val dynamicColorContext = DynamicColors.wrapContextIfAvailable(this, com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight)
    val typedValue = dynamicColorContext.obtainStyledAttributes(intArrayOf(attrRes))
    val color = typedValue.getColor(0, 0)
    typedValue.recycle()
    return color
}