package com.ojhdtapp.parabox.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings


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

fun Context.launchSetting(){
    val intent = Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}