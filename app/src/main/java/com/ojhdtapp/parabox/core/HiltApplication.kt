package com.ojhdtapp.parabox.core

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), DefaultLifecycleObserver, Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    companion object {
        var inBackground: Boolean = false
    }

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    @Override
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        inBackground = false
    }

    @Override
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        inBackground = true
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}