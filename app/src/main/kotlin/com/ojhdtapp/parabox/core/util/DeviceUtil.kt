package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
object DeviceUtil {
    fun isHonorDevice() = Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
    fun isXiaomiDevice() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    fun isOppoDevice() = Build.MANUFACTURER.equals("OPPO", ignoreCase = true)
    fun isOnePlusDevice() = Build.MANUFACTURER.equals("OnePlus", ignoreCase = true)
    fun isRealmeDevice() = Build.MANUFACTURER.equals("realme", ignoreCase = true)
    fun isVivoDevice() = Build.MANUFACTURER.equals("vivo", ignoreCase = true)
    fun isHuaweiDevice() = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)
    fun isMIUI(context: Context): Boolean {
        try {
            val pm = context.packageManager
            pm.getPackageInfo("com.miui.securitycenter", PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return false
    }
}