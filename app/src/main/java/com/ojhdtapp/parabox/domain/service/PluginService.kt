package com.ojhdtapp.parabox.domain.service

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.plugin.PluginConnObj
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PluginService : LifecycleService() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage
    private var installedPluginList = emptyList<ApplicationInfo>()
    private var appModelList = emptyList<AppModel>()
    private val pluginConnectionMap = mutableMapOf<String, PluginConnObj>()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return PluginServiceBinder()
    }

    inner class PluginServiceBinder : Binder() {
        fun getService(): PluginService {
            return this@PluginService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get Installed Plugin
//        val installedPluginList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        installedPluginList = packageManager.queryIntentServices(Intent().apply {
            action = "com.ojhdtapp.parabox.PLUGIN"
        }, PackageManager.GET_META_DATA).map {
            it.serviceInfo.applicationInfo
        }
        appModelList = installedPluginList.map {

            AppModel(
                name = it.loadLabel(packageManager).toString(),
                icon = it.loadIcon(packageManager),
                packageName = it.packageName,
                version = packageManager.getPackageInfo(
                    it.packageName,
                    PackageManager.GET_META_DATA
                ).versionName,
                launchIntent = packageManager.getLaunchIntentForPackage(it.packageName),
                runningStatus = AppModel.RUNNING_STATUS_CHECKING,
                connectionType = it.metaData?.getInt("connection_type") ?: 0
            )
        }
        bindPlugins()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        pluginConnectionMap.forEach {
            unbindService(it.value.getServiceConnection())
        }
        super.onDestroy()
    }

    // Call Only Once
    private fun bindPlugins() {
        installedPluginList.forEach {
            lifecycleScope.launch {
                val pluginConnObj = PluginConnObj(
                    {
                        lifecycleScope.launch(Dispatchers.IO){
                            handleNewMessage(it)
                        }

                    },
                    this@PluginService,
                    it.packageName,
                    it.packageName + ".domain.service.ConnService"
                )
                pluginConnectionMap.put(it.packageName, pluginConnObj)
                pluginConnObj.connect()
                pluginConnObj.refreshRunningStatus()
            }
        }
    }

    fun getPluginListFlow(): Flow<List<AppModel>> {
        Log.d("parabox", "begin creating flow")
        return flow {
            while (true) {
                emit(appModelList.map {
                    val connObj = pluginConnectionMap[it.packageName]
                    Log.d(
                        "parabox",
                        "status:${connObj?.getRunningStatus() ?: AppModel.RUNNING_STATUS_DISABLED}"
                    )
                    connObj?.refreshRunningStatus()
                    it.copy(
                        runningStatus = connObj?.getRunningStatus()
                            ?: AppModel.RUNNING_STATUS_DISABLED
                    )
                })
                delay(2000)
            }
        }
    }
}
