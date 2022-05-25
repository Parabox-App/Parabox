package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService

class BatteryUtil(val context: Context) {
    private fun isBatteryOptimizationIgnored(): Boolean =
        context.getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(context.packageName) ?: false


    fun ignoreBatteryOptimization() {
        if (isBatteryOptimizationIgnored()) {
            context.startActivity(Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            })
        } else {
            context.startActivity(Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }
}