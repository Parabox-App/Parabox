package com.ojhdtapp.parabox.domain.plugin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import javax.inject.Singleton

class Conn(private val ctx: Context) {
    init{

    }

    fun connect(pkg: String, cls: String) {
        val intent = Intent().apply {
            component = ComponentName(
                pkg, cls
            )
        }
        ctx.startService(intent)
    }

    fun isInstalled(pkg: String): Boolean {
        var res = false
        val pkManager = ctx.packageManager
        try {
            pkManager.getPackageInfo(pkg, 0)
            res = true
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return res
    }

}