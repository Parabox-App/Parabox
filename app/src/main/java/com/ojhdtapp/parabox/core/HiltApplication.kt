package com.ojhdtapp.parabox.core

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import top.canyie.pine.BuildConfig
import top.canyie.pine.Pine
import top.canyie.pine.Pine.CallFrame
import top.canyie.pine.PineConfig
import top.canyie.pine.callback.MethodHook
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override fun onCreate() {
        super.onCreate()
        PineConfig.debug = true; // 是否debug，true会输出较详细log
        PineConfig.debuggable = BuildConfig.DEBUG; // 该应用是否可调试，建议和配置文件中的值保持一致，否则会出现问题
        Pine.hook(Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java), object : MethodHook() {
            override fun beforeCall(callFrame: CallFrame) {
                Log.i("aaa", "Before " + callFrame.thisObject + " onCreate()")
            }

            override fun afterCall(callFrame: CallFrame) {
                Log.i("aaa", "After " + callFrame.thisObject + " onCreate()")
            }
        })
        Pine.hook(
            Instrumentation::class.java.getDeclaredMethod(
                "execStartActivity",
                Context::class.java,
                IBinder::class.java,
                IBinder::class.java,
                String::class.java,
                Intent::class.java,
                Int::class.java,
                Bundle::class.java
            ), object : MethodHook() {
                override fun beforeCall(callFrame: CallFrame?) {
                    Log.d("aaa", "before: ${callFrame!!.thisObject}")
                }

                override fun afterCall(callFrame: CallFrame?) {
                    Log.d("aaa", "after: ${callFrame!!.thisObject}")
                }
            })
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}