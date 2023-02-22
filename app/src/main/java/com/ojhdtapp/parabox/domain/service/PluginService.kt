package com.ojhdtapp.parabox.domain.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.plugin.PluginConnObj
import com.ojhdtapp.parabox.domain.use_case.DeleteMessage
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class PluginService : LifecycleService() {
    companion object {
        const val REPLY_ACTION = "reply_action"

        val pluginTypeMap = mutableMapOf<Int, String>()
        fun queryPluginConnectionName(type: Int): String {
            return pluginTypeMap[type] ?: "Unknown"
        }

        fun getReplyIntent(context: Context, dto: SendMessageDto): Intent {
            return Intent(context, PluginService::class.java).apply {
                action = REPLY_ACTION
                putExtra("dto", dto)
            }
        }
    }

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var updateMessage: UpdateMessage

    @Inject
    lateinit var deleteMessage: DeleteMessage

    @Inject
    lateinit var notificationUtil: NotificationUtil

    private var installedPluginList = emptyList<ApplicationInfo>()
    private var appModelList = emptyList<AppModel>()
    private val pluginConnectionMap = mutableMapOf<Int, PluginConnObj>()
    private var pluginListListener: PluginListListener? = null

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        // Update List When Rebind
        pluginListListener?.onPluginListChange(
            appModelList
        )
        return PluginServiceBinder()
    }

    inner class PluginServiceBinder : Binder() {
        fun getService(): PluginService {
            return this@PluginService
        }
    }

    override fun onCreate() {
        Log.d("parabox", "PluginService onCreate")
        super.onCreate()
        // Foreground Service
        try {
            notificationUtil.startForegroundService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Query Plugins and Bind
        updateInstalledPluginList()
        updateAppModelList()
        bindPlugins()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == REPLY_ACTION) {
            val dto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("dto", SendMessageDto::class.java)
            } else {
                intent.getParcelableExtra<SendMessageDto>("dto")
            }
            if (dto != null) {
                sendMessage(dto)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("parabox", "onDestroy called")
        unbindPlugins()
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }

    fun getAppModelList() : List<AppModel> = appModelList.toList()

    // Call Only Once
    private fun bindPlugins() {
        appModelList.forEach { appModel ->
            val pluginConnObj = PluginConnObj(
                ctx = this,
                coroutineScope = lifecycleScope,
                pkg = appModel.packageName,
                cls = appModel.packageName + ".domain.service.ConnService",
                connectionType = appModel.connectionType,
                handleNewMessage = handleNewMessage,
                updateMessage = updateMessage,
                deleteMessage = deleteMessage,
                onRunningStatusChange = { connectionType, status ->
                    Log.d("parabox", "stated changed received: $status")
                    appModelList = appModelList.map {
                        if (it.connectionType == connectionType) it.copy(runningStatus = status) else it
                    }.also {
                        // Send Update to Activity
                        pluginListListener?.onPluginListChange(it)
                        // Send Notification
                        val connectingPluginNum =
                            it.count { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }
                        notificationUtil.updateForegroundServiceNotification(
                            if (connectingPluginNum == 0) "没有已连接的扩展" else "已连接 $connectingPluginNum 个扩展",
                            "Parabox 正在后台运行",
                        )
                    }
                }
            )
            pluginConnectionMap[appModel.connectionType] = pluginConnObj
            pluginConnObj.connect()
            notificationUtil.createNotificationChannel(
                appModel.connectionType.toString(),
                appModel.connectionName,
                "来自插件 ${appModel.connectionName} 的消息",
                NotificationManager.IMPORTANCE_HIGH
            )
        }
    }

    private fun unbindPlugins() {
        pluginConnectionMap.forEach {
            it.value.disconnect()
        }
        pluginConnectionMap.clear()
        installedPluginList = emptyList<ApplicationInfo>()
        appModelList = emptyList<AppModel>()
    }

    private fun updateInstalledPluginList(){
        installedPluginList = packageManager.queryIntentServices(Intent().apply {
            action = "com.ojhdtapp.parabox.PLUGIN"
        }, PackageManager.GET_META_DATA).map {
            it.serviceInfo.applicationInfo
        }
    }

    private fun updateAppModelList() {
        appModelList = installedPluginList.map {
            val connectionType = it.metaData?.getInt("connection_type") ?: 0
            val connectionName = it.metaData?.getString("connection_name") ?: "Unknown"
            PluginService.pluginTypeMap[connectionType] = connectionName

            val author = it.metaData?.getString("author") ?: "Unknown"
            val description = it.metaData?.getString("description")?: "Null"
            val plainTextSupport = it.metaData?.getInt("plain_text_support") ?: AppModel.SUPPORT_NULL
            val imageSupport = it.metaData?.getInt("image_support") ?: AppModel.SUPPORT_NULL
            val audioSupport = it.metaData?.getInt("audio_support") ?: AppModel.SUPPORT_NULL
            val fileSupport = it.metaData?.getInt("file_support") ?: AppModel.SUPPORT_NULL
            val atSupport = it.metaData?.getInt("at_support") ?: AppModel.SUPPORT_NULL
            val quoteReplySupport = it.metaData?.getInt("quote_reply_support") ?: AppModel.SUPPORT_NULL

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
                connectionName = connectionName,
                author = author,
                description = description,
                plainTextSupport = plainTextSupport,
                imageSupport = imageSupport,
                audioSupport = audioSupport,
                fileSupport = fileSupport,
                atSupport = atSupport,
                quoteReplySupport = quoteReplySupport
            )
        }
    }

    fun setPluginListListener(listener: PluginListListener) {
        pluginListListener = listener
    }

//    fun getPluginListFlow(): Flow<List<AppModel>> {
//        Log.d("parabox", "begin creating flow")
//        return flow {
//            while (true) {
//                emit(appModelList.map {
//                    val connObj = pluginConnectionMap[it.connectionType]
//                    Log.d(
//                        "parabox",
//                        "status:${connObj?.getRunningStatus() ?: AppModel.RUNNING_STATUS_DISABLED}"
//                    )
//                    it.copy(
//                        runningStatus = connObj?.getRunningStatus()
//                            ?: AppModel.RUNNING_STATUS_DISABLED
//                    )
//                })
//                delay(2000)
//            }
//        }
//    }

    fun sendMessage(dto: SendMessageDto) {
        val type = dto.pluginConnection.connectionType
        if (appModelList.map { it.connectionType }.contains(type)) {
            pluginConnectionMap[type]?.send(dto)
        } else {
            if (Looper.myLooper() == null)
            {
                Looper.prepare();
            }
            Toast.makeText(this, "插件未安装", Toast.LENGTH_SHORT).show()
        }
    }

    fun recallMessage(type: Int, messageId: Long) {
        if (appModelList.map { it.connectionType }.contains(type)) {
            pluginConnectionMap[type]?.recall(messageId)
        } else {
            if (Looper.myLooper() == null)
            {
                Looper.prepare();
            }
            Toast.makeText(this, "插件未安装", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun refreshMessage(): Boolean {
        return try {
            withTimeout(5000) {
                pluginConnectionMap.map {
                    it.value.refreshMessage()
                }.let { it ->
                    it.all { it is ParaboxResult.Success }
                }
            }
        } catch (e: TimeoutCancellationException) {
            false
        }
    }

    fun reset(){
        unbindPlugins()
        updateInstalledPluginList()
        updateAppModelList()
        bindPlugins()
    }

    fun stop() {
        unbindPlugins()
    }
}

