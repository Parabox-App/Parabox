package com.ojhdtapp.parabox.domain.service

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.plugin.PluginConnObj
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
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
    companion object {
        val pluginTypeMap = mutableMapOf<Int, String>()
        fun queryPluginConnectionName(type: Int): String {
            return pluginTypeMap[type] ?: "Unknown"
        }
    }

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var updateMessage: UpdateMessage
    private var installedPluginList = emptyList<ApplicationInfo>()
    private var appModelList = emptyList<AppModel>()
    private val pluginConnectionMap = mutableMapOf<Int, PluginConnObj>()

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
            val connectionType = it.metaData?.getInt("connection_type") ?: 0
            val connectionName = it.metaData?.getString("connection_name") ?: "Unknown"
            PluginService.pluginTypeMap.put(connectionType, connectionName)
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
                connectionType = connectionType,
                connectionName = connectionName
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
        appModelList.forEach {
            val pluginConnObj = PluginConnObj(
                {
                    lifecycleScope.launch(Dispatchers.IO) {
                        handleNewMessage(it)
                    }

                },
                { id: Long, value: Boolean ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        Log.d("parabox", "response received: ${id}")
                        updateMessage.verifiedState(id, value)
                    }
                },
                this@PluginService,
                it.packageName,
                it.packageName + ".domain.service.ConnService"
            )
            pluginConnectionMap.put(it.connectionType, pluginConnObj)
            pluginConnObj.connect()
            pluginConnObj.refreshRunningStatus()
        }
    }

    fun getPluginListFlow(): Flow<List<AppModel>> {
        Log.d("parabox", "begin creating flow")
        return flow {
            while (true) {
                emit(appModelList.map {
                    val connObj = pluginConnectionMap[it.connectionType]
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

    fun sendMessage(dto: SendMessageDto) {
        val type = dto.pluginConnection.connectionType
        if (appModelList.map { it.connectionType }.contains(type)) {
            pluginConnectionMap[type]?.send(dto)
        } else {
            Toast.makeText(this, "插件未安装", Toast.LENGTH_SHORT).show()
        }
    }

    fun recallMessage(type: Int, messageId: Long){
        if (appModelList.map { it.connectionType }.contains(type)) {
            pluginConnectionMap[type]?.recall(messageId)
        } else {
            Toast.makeText(this, "插件未安装", Toast.LENGTH_SHORT).show()
        }
    }
}

