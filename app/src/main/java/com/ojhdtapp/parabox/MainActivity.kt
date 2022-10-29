package com.ojhdtapp.parabox

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.api.services.drive.DriveScopes
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.ojhdtapp.parabox.core.util.BrowserUtil
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.DownloadManagerUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.data.remote.dto.saveLocalResourcesToCloud
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.service.PluginListListener
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import com.ojhdtapp.parabox.domain.use_case.GetFiles
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.use_case.UpdateFile
import com.ojhdtapp.parabox.domain.use_case.UpdateMessage
import com.ojhdtapp.parabox.domain.worker.CleanUpFileWorker
import com.ojhdtapp.parabox.domain.worker.DownloadFileWorker
import com.ojhdtapp.parabox.domain.worker.UploadFileWorker
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FixedInsets
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        var inBackground: Boolean = false
    }

    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var updateFile: UpdateFile

    @Inject
    lateinit var getFiles: GetFiles

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @Inject
    lateinit var getContacts: GetContacts

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var fcmApiHelper: FcmApiHelper

    @Inject
    lateinit var updateMessage: UpdateMessage

    var pluginService: PluginService? = null
    private lateinit var pluginServiceConnection: ServiceConnection
    private lateinit var userAvatarPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var userAvatarPickerSLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var recorder: MediaRecorder? = null
    private var recorderJob: Job? = null
    private var recorderStartTime: Long? = null
    private lateinit var recordPath: String
    private var player: MediaPlayer? = null
    private var playerJob: Job? = null
    private var amplituda: Amplituda? = null

    lateinit var vibrator: Vibrator
    lateinit var backup: RoomBackup

    private lateinit var analytics: FirebaseAnalytics

    // Shared ViewModel
    val mainSharedViewModel by viewModels<MainSharedViewModel>()

    // Backup and Restore
    private lateinit var backupLocationSelector: ActivityResultLauncher<String>
    private lateinit var restoreLocationSelector: ActivityResultLauncher<Array<String>>

    private fun openFile(file: File) {
        file.downloadPath?.let {
            val path = java.io.File(
                Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                it
            )
            FileUtil.openFile(this, path, file.extension)
        }
    }

    private fun downloadFile(file: File, cloudFirst: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (file.cloudType) {
                GoogleDriveUtil.SERVICE_CODE -> file.cloudId?.let {
                    GoogleDriveUtil.downloadFile(
                        baseContext,
                        it,
                        java.io.File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "Parabox"
                        ),
                    )
                }

                else -> {
                    val url = file.url
                    if (url != null) {
                        val path = FileUtil.getAvailableFileName(this@MainActivity, file.name)
                        DownloadManagerUtil.downloadWithManager(
                            this@MainActivity,
                            url,
                            path
                        )?.also {
                            updateFile.downloadInfo(path, it, file)
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                DownloadManagerUtil.retrieve(this@MainActivity, it).collectLatest {
                                    if (it is DownloadingState.Done) {
                                        updateFile.downloadInfo(path, null, file)
                                    }
                                    updateFile.downloadState(it, file)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun retrieveDownloadProcess(file: File) {
        file.downloadId?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    DownloadManagerUtil.retrieve(this@MainActivity, id).collectLatest {
                        updateFile.downloadState(it, file)
                    }
                }
            }
        }
    }

    private fun startPlayingLocal(uri: Uri) {
        stopPlaying()
        player = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, uri)
                setOnPreparedListener {
                    playerJob = lifecycleScope.launch {
                        while (true) {
                            val progress = (currentPosition.toFloat() / duration)
                            mainSharedViewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(this@MainActivity).also { amplituda ->
                        amplituda.processAudio(
                            FileUtil.uriToTempFile(this@MainActivity, uri),
                            Compress.withParams(Compress.AVERAGE, 2)
                        ).get(
                            { result ->
                                mainSharedViewModel.insertAllIntoRecordAmplitudeStateList(
                                    result.amplitudesAsList().map { it * 1000 })
                            }, { exception ->
                                exception.printStackTrace()
                            })
                    }
                    mainSharedViewModel.setIsAudioPlaying(true)
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    mainSharedViewModel.clearRecordAmplitudeStateList()
                    mainSharedViewModel.setIsAudioPlaying(false)
                }
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalStateException){
                e.printStackTrace()
            }
        }


//        player?.let {
//            Visualizer(it.audioSessionId).apply {
//                captureSize = Visualizer.getCaptureSizeRange()[1]
//                setDataCaptureListener(object: Visualizer.OnDataCaptureListener{
//                    override fun onWaveFormDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {
//                        val amplitude = p1?.let { it1 -> calculateRMSLevel(it1) } ?: 0
//                        Log.d("parabox", "WaveFromData:$amplitude")
//                    }
//                    override fun onFftDataCapture(p0: Visualizer?, p1: ByteArray?, p2: Int) {
//
//                    }
//                },Visualizer.getMaxCaptureRate() / 2, true, false)
//                enabled = true
//            }
//        }
    }

    private fun startPlayingInternet(url: String) {
        stopPlaying()
        player = MediaPlayer().apply {
            try {
                setDataSource(url)
                setOnPreparedListener {
                    playerJob = lifecycleScope.launch {
                        while (true) {
                            val progress = (currentPosition.toFloat() / duration)
                            mainSharedViewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(this@MainActivity).also { amplituda ->
                        amplituda.processAudio(
                            url,
                            Compress.withParams(Compress.AVERAGE, 2)
                        ).get(
                            { result ->
                                mainSharedViewModel.insertAllIntoRecordAmplitudeStateList(
                                    result.amplitudesAsList().map { it * 1000 })
                            }, { exception ->
                                exception.printStackTrace()
                            })
                    }
                    mainSharedViewModel.setIsAudioPlaying(true)
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    mainSharedViewModel.clearRecordAmplitudeStateList()
                    mainSharedViewModel.setIsAudioPlaying(false)
                }
                prepareAsync()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
        }
    }

//    fun calculateRMSLevel(audioData: ByteArray): Int {
//        var amplitude = 0.0
//        for (i in audioData.indices) {
//            amplitude += Math.abs((audioData[i] / 32768.0))
//        }
//        amplitude /= audioData.size
//
//        return amplitude.toInt()
//    }

    fun calculateRMSLevel(audioData: ByteArray): Double {
        var amplitude = 0.0
        for (i in 0 until (audioData.size / 2)) {
            val y = (audioData[i * 2].toInt() or (audioData[i * 2 + 1].toInt() shl 8)) / 32768.0
            amplitude += abs(y)
        }
        amplitude = amplitude / audioData.size / 2
        return amplitude
    }

    private fun stopPlaying() {
        playerJob?.cancel()
        playerJob = null
        player?.release()
        player = null
        mainSharedViewModel.setIsAudioPlaying(false)
    }

    private fun pausePlaying() {
        if (player?.isPlaying == true) {
            player?.pause()
            mainSharedViewModel.setIsAudioPlaying(false)
        }
    }

    private fun resumePlaying() {
        if (player?.isPlaying == false) {
            player?.start()
            mainSharedViewModel.setIsAudioPlaying(true)
        }
    }

    private fun setProgress(fraction: Float) {
        player?.run {
            seekTo((duration * fraction).roundToInt())
        }
    }

    private fun startRecording() {
        recorderJob?.cancel()
        if (recorderJob != null)
            recorderJob = null
        mainSharedViewModel.clearRecordAmplitudeStateList()
        recorderStartTime = System.currentTimeMillis()
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(recordPath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
            start()
        }
        recorderJob = lifecycleScope.launch {
            while (true) {
                val value = recorder?.maxAmplitude ?: 0
                Log.d("parabox", "$value")
                mainSharedViewModel.insertIntoRecordAmplitudeStateList(value)
                delay(500)
            }
        }
    }

    private fun stopRecording() {
        lifecycleScope.launch {
            if (abs(
                    (recorderStartTime ?: System.currentTimeMillis()) - System.currentTimeMillis()
                ) < 300
            ) {
                delay(300)
            }
            recorder?.apply {
                stop()
                reset()
                release()
            }
            recorderJob?.cancel()
            if (recorderJob != null)
                recorderJob = null
            recorder = null
            recorderStartTime = null
        }
    }

    private fun pickUserAvatar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
            userAvatarPickerLauncher.launch(intent)
        } else {
            userAvatarPickerSLauncher.launch("image/*")
        }
    }

    private fun setUserAvatar(uri: Uri) {
        getExternalFilesDir("avatar")?.listFiles()?.filter { it.isFile }?.map {
            it.delete()
        }
        val path = getExternalFilesDir("avatar")!!
        val copiedUri = FileUtil.getUriByCopyingFileToPath(
            this,
            path,
            "${System.currentTimeMillis().toDateAndTimeString()}.jpg",
            uri
        )
//        val outputFile =
//            File("${getExternalFilesDir("avatar")}${File.separator}AVATAR_$timeStr.jpg")
//        contentResolver.openInputStream(uri)?.use { inputStream ->
//            FileOutputStream(outputFile).use { outputStream ->
//                inputStream.copyTo(outputStream)
//            }
//        }
//        val copiedUri = FileProvider.getUriForFile(
//            this,
//            BuildConfig.APPLICATION_ID + ".provider", outputFile
//        )
        copiedUri?.let {
            lifecycleScope.launch {
                this@MainActivity.dataStore.edit { settings ->
                    settings[DataStoreKeys.USER_AVATAR] = it.toString()
                }
                Toast.makeText(this@MainActivity, "头像已更新", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 20))
        }
    }

    private fun refreshMessage() {
        mainSharedViewModel.setIsRefreshing(true)
        lifecycleScope.launch {
            val fcmRole = dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_ROLE] ?: FcmConstants.Role.SENDER.ordinal
            }.first()
            when (fcmRole) {
                FcmConstants.Role.SENDER.ordinal -> {
                    if (pluginService?.refreshMessage() == true) {
                        delay(500)
                        mainSharedViewModel.setIsRefreshing(false)
                    } else {
                        delay(500)
                        mainSharedViewModel.setIsRefreshing(false)
                        Toast.makeText(this@MainActivity, "通信时发生错误", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                FcmConstants.Role.RECEIVER.ordinal -> {

                }
            }
        }
    }

    private fun backupDatabase() {
        backup
            .database(appDatabase)
            .enableLogDebug(true)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_EXTERNAL)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    if (success) {
                        Toast.makeText(this@MainActivity, "请选择存储路径", Toast.LENGTH_SHORT)
                            .show()
                        backupLocationSelector.launch(
                            "Backup_${
                                System.currentTimeMillis().toDateAndTimeString()
                            }.sqlite3"
                        )
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "备份失败 错误代码:${exitCode}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .backup()
    }

    private fun restoreDatabase(file: java.io.File) {
        backup
            .database(appDatabase)
            .enableLogDebug(true)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
            .backupLocationCustomFile(file)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    if (success) {
                        Toast.makeText(this@MainActivity, "恢复完成 即将重启", Toast.LENGTH_SHORT)
                            .show()
                        file.delete()
                        lifecycleScope.launch {
                            delay(1000)
//                            restartApp(Intent(this@MainActivity, MainActivity::class.java))
                            onEvent(ActivityEvent.RestartApp)
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "恢复失败 错误代码:${exitCode}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .restore()
    }

    private fun resetPluginConnection() {
        pluginService?.also {
            it.reset()
            Toast.makeText(this, "已重置扩展连接", Toast.LENGTH_SHORT).show()
        }

    }

    fun backupFileToCloudService() {
        val workManager = WorkManager.getInstance(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val enableAutoBackup =
                dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_BACKUP] ?: false
            val defaultBackupService =
                dataStore.data.first()[DataStoreKeys.SETTINGS_DEFAULT_BACKUP_SERVICE] ?: 0
            val autoBackupFileMaxSize =
                (dataStore.data.first()[DataStoreKeys.SETTINGS_AUTO_BACKUP_FILE_MAX_SIZE]
                    ?: 10f).let {
                    if (it == 100f) Long.MAX_VALUE
                    else it.toLong() * 1024 * 1024
                }
            if (enableAutoBackup && defaultBackupService != 0) {
                val files = getContacts.shouldBackup().map { it.contactId }.let {
                    getFiles.byContactIdsStatic(it).filter { it.size < autoBackupFileMaxSize }
                }
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
//                    .setRequiresDeviceIdle(true)
                    .build()
                files.forEach {
                    val tag = it.fileId.toString()
                    val downloadRequest = OneTimeWorkRequestBuilder<DownloadFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "url" to it.url,
                                "name" to it.name,
                            )
                        )
                        .build()
                    val uploadRequest = OneTimeWorkRequestBuilder<UploadFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "default_backup_service" to defaultBackupService,
                            )
                        )
                        .build()
                    val cleanUpRequest = OneTimeWorkRequestBuilder<CleanUpFileWorker>()
                        .setConstraints(constraints)
                        .addTag(tag)
                        .setInputData(
                            workDataOf(
                                "fileId" to it.fileId,
                            )
                        )
                        .build()
                    val continuation = workManager.beginUniqueWork(
                        tag,
                        ExistingWorkPolicy.KEEP,
                        downloadRequest
                    )
                        .then(uploadRequest)
                        .then(cleanUpRequest)
                    continuation.enqueue()
                    launch(Dispatchers.Main) {
                        files.forEach {
                            workManager.getWorkInfosByTagLiveData(tag)
                                .observe(this@MainActivity) { workInfoList ->
                                    mainSharedViewModel.putWorkInfo(tag, it, workInfoList)
                                }
                        }
//                        continuation.workInfosLiveData.observe(this@MainActivity) { workInfoList ->
//                            mainSharedViewModel.putWorkInfo(it, workInfoList)
//                        }
                    }
                }
            }
        }
    }

    fun backupFileToCloudService(file: File) {
        val workManager = WorkManager.getInstance(this)

        lifecycleScope.launch(Dispatchers.IO) {
            updateFile.cloudInfo(null, null, file.fileId)
            val defaultBackupService =
                dataStore.data.first()[DataStoreKeys.SETTINGS_DEFAULT_BACKUP_SERVICE] ?: 0
            if (defaultBackupService != 0) {
                val tag = file.fileId.toString()
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
                val downloadRequest = OneTimeWorkRequestBuilder<DownloadFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "url" to file.url,
                            "name" to file.name,
                        )
                    )
                    .build()
                val uploadRequest = OneTimeWorkRequestBuilder<UploadFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "default_backup_service" to defaultBackupService,
                        )
                    )
                    .build()
                val cleanUpRequest = OneTimeWorkRequestBuilder<CleanUpFileWorker>()
                    .setConstraints(constraints)
                    .addTag(tag)
                    .setInputData(
                        workDataOf(
                            "fileId" to file.fileId,
                        )
                    )
                    .build()
                val continuation = workManager.beginUniqueWork(
                    tag,
                    ExistingWorkPolicy.KEEP,
                    downloadRequest
                )
                    .then(uploadRequest)
                    .then(cleanUpRequest)
                continuation.enqueue()
                launch(Dispatchers.Main) {
                    continuation.workInfosLiveData.observe(this@MainActivity) { workInfoList ->
                        mainSharedViewModel.putWorkInfo(tag, file, workInfoList)
                    }
                }
            }
        }
    }

    fun cancelBackupWorkByTag(tag: String) {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelAllWorkByTag(tag)
    }

    private fun queryFCMToken() {
        lifecycleScope.launch {
            if (dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] == true) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("parabox", "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    Log.d("parabox", "FCM token: $token")
                    lifecycleScope.launch {
                        dataStore.edit { settings ->
                            settings[DataStoreKeys.FCM_TOKEN] = token
                        }
                    }
                })
            }
        }
    }

    fun getGoogleLoginAuth(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(DriveScopes.DRIVE),
                Scope(DriveScopes.DRIVE_APPDATA),
                Scope(DriveScopes.DRIVE_FILE),
            )
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    fun getGoogleDriveInformation() {
        lifecycleScope.launch {
            GoogleDriveUtil.getDriveInformation(this@MainActivity)?.also {
                this@MainActivity.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.GOOGLE_WORK_FOLDER_ID] = it.workFolderId
                    preferences[DataStoreKeys.GOOGLE_TOTAL_SPACE] = it.totalSpace
                    preferences[DataStoreKeys.GOOGLE_USED_SPACE] = it.usedSpace
                    preferences[DataStoreKeys.GOOGLE_APP_USED_SPACE] = it.appUsedSpace
                }
            }
        }
    }

    // Event
    fun onEvent(event: ActivityEvent) {
        when (event) {
            is ActivityEvent.LaunchIntent -> {
                startActivity(event.intent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }

            is ActivityEvent.LaunchURL -> {
                BrowserUtil.launchURL(this, event.url)
            }

            is ActivityEvent.RestartApp -> {
                val ctx = applicationContext
                val pm = ctx.packageManager
                val intent = pm.getLaunchIntentForPackage(ctx.packageName)
                val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
                ctx.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }

            is ActivityEvent.SendMessage -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val timestamp = System.currentTimeMillis()
                    handleNewMessage(
                        event.contents,
                        event.pluginConnection,
                        timestamp,
                        event.sendType
                    ).also {
                        val dto = SendMessageDto(
                            contents = event.contents,
                            timestamp = timestamp,
                            pluginConnection = event.pluginConnection,
                            messageId = it
                        )

                        val enableFcm =
                            dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
                        val fcmRole = dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_ROLE]
                            ?: FcmConstants.Role.SENDER.ordinal
                        if (!enableFcm || fcmRole == FcmConstants.Role.SENDER.ordinal) {
                            pluginService?.sendMessage(dto)
                        } else {
                            val fcmCloudStorage =
                                dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE]
                                    ?: FcmConstants.CloudStorage.NONE.ordinal
                            val dtoWithoutUri = when {
                                fcmCloudStorage == FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> {
                                    dto.copy(
                                        contents = dto.contents.saveLocalResourcesToCloud(baseContext)
                                    )
                                }

                                else -> dto
                            }
                            if (fcmApiHelper.pushSendDto(
                                    dtoWithoutUri
                                )?.isSuccessful == true
                            ) {
                                updateMessage.verifiedState(it, true)
                                Log.d("parabox", "FCM push success")
                            } else {
                                updateMessage.verifiedState(it, false)
                                Log.d("parabox", "FCM push failed")
                            }
                        }
                    }
                }
            }

            is ActivityEvent.RecallMessage -> {
                pluginService?.recallMessage(event.type, event.messageId)
            }

            is ActivityEvent.SetUserAvatar -> {
                pickUserAvatar()
            }

            is ActivityEvent.StartRecording -> {
                if (player?.isPlaying == true) {
                    stopPlaying()
                    Toast.makeText(this, "播放中的音频已中断", Toast.LENGTH_SHORT).show()
                }
                startRecording()
            }

            is ActivityEvent.StopRecording -> {
                stopRecording()
            }

            is ActivityEvent.StartAudioPlaying -> {
                if (recorder != null) {
                    Toast.makeText(this, "请先结束录音", Toast.LENGTH_SHORT).show()
                } else {
                    if (event.uri != null) {
                        startPlayingLocal(event.uri)
                    } else if (event.url != null) {
                        startPlayingInternet(event.url)
                    } else {
                        Toast.makeText(this, "音频资源丢失", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is ActivityEvent.StopAudioPlaying -> {
                stopPlaying()
            }

            is ActivityEvent.PauseAudioPlaying -> {
                pausePlaying()
            }

            is ActivityEvent.ResumeAudioPlaying -> {
                resumePlaying()
            }

            is ActivityEvent.SetAudioProgress -> {
                setProgress(event.fraction)
            }

            is ActivityEvent.DownloadFile -> {
                downloadFile(event.file)
            }

            is ActivityEvent.DownloadCloudFile -> {
                downloadFile(event.file, true)
            }

            is ActivityEvent.OpenFile -> {
                openFile(event.file)
            }

            is ActivityEvent.Vibrate -> {
                vibrate()
            }

            is ActivityEvent.RefreshMessage -> {
                refreshMessage()
            }

            is ActivityEvent.ShowInBubble -> {
                lifecycleScope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && notificationUtil.canBubble(
                            event.contact,
                            event.channelId
                        )
                    ) {
                        notificationUtil.sendNewMessageNotification(
                            event.message,
                            event.contact,
                            event.channelId,
                            true,
                            true
                        )
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "当前会话未启用对话泡",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            is ActivityEvent.Backup -> {
                backupDatabase()
            }

            is ActivityEvent.Restore -> {
                restoreLocationSelector.launch(
                    arrayOf(
                        "application/vnd.sqlite3",
                        "application/x-sqlite3",
                        "application/octet-stream",
                        "application/x-trash",
                    )
                )
            }

            is ActivityEvent.ResetExtension -> {
                resetPluginConnection()
            }

            is ActivityEvent.SaveToCloud -> {
                backupFileToCloudService(event.file)
            }

            is ActivityEvent.CancelBackupWork -> {
                cancelBackupWorkByTag(event.tag)
                mainSharedViewModel.workInfoMap.remove(event.tag)
                lifecycleScope.launch(Dispatchers.IO) {
                    updateFile.cloudInfo(0, null, event.fileId)
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            // Invoked when a dynamic shortcut is clicked.
            Intent.ACTION_VIEW -> {
                val id = intent.data?.lastPathSegment?.toLongOrNull()
                if (id != null) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            getContacts.queryById(id)
                        }.also {
                            if (it != null) {
                                mainSharedViewModel.navigateToChatPage(it)
                            }
                        }
                    }
                }
            }
            // Invoked when a text is shared through Direct Share.
            Intent.ACTION_SEND -> {
                val shortcutId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                shortcutId?.toLong()?.let { id ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            getContacts.queryById(id)
                        }.also {
                            if (it != null) {
                                mainSharedViewModel.navigateToChatPage(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Navigate to Page
        if (savedInstanceState == null) {
            intent?.let(::handleIntent)
        }

        // Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Record
        recordPath = "${externalCacheDir!!.absoluteFile}/audio_record.mp3"
//        recordPath = "${getExternalFilesDir("chat")!!.absoluteFile}/audio_record.mp3"

        // Activity Result Api
        userAvatarPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    it.data?.data?.let {
                        setUserAvatar(it)
                    }
                }
            }
        userAvatarPickerSLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) {
                it?.let {
                    setUserAvatar(it)
                }
            }

        // Request Permission Launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

        backupLocationSelector =
            registerForActivityResult(CreateDocument("application/vnd.sqlite3")) { uri ->
                if (uri != null) {
                    getExternalFilesDir("backup")?.also { dir ->
                        dir.listFiles()?.firstOrNull()?.also { file ->
                            contentResolver.openOutputStream(uri)?.use { output ->
                                file.inputStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            file.delete()
                        }
                        Toast.makeText(this, "备份已完成", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "操作取消", Toast.LENGTH_SHORT).show()
                }

            }

        restoreLocationSelector =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    contentResolver.openInputStream(uri)?.use { input ->
                        getExternalFilesDir("backup")?.also { dir ->
                            dir.listFiles()?.forEach { it.delete() }
                            val file = java.io.File(dir, "chat.db")
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            restoreDatabase(file = file)
                        }
                    }
                }
            }

        // File Download Process
        lifecycleScope.launch(Dispatchers.IO) {
            getFiles.allStatic().forEach {
                if (it.downloadPath == null) {
                    updateFile.downloadState(DownloadingState.None, it)
                    updateFile.downloadInfo(null, null, it)
                } else {
                    val path = java.io.File(
                        Environment.getExternalStoragePublicDirectory("${Environment.DIRECTORY_DOWNLOADS}/Parabox"),
                        it.downloadPath
                    )
                    if (!path.exists()) {
                        updateFile.downloadState(DownloadingState.None, it)
                        updateFile.downloadInfo(null, null, it)
                    } else {
                        retrieveDownloadProcess(it)
                    }
                }
            }
        }

        // Google Drive
        getGoogleDriveInformation()

        // Backup
        backup = RoomBackup(this)

        // Cloud Backup
        backupFileToCloudService()

        // Obtain the FirebaseAnalytics instance.
        analytics = Firebase.analytics

        // Query FCM Token
        queryFCMToken()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            // System Ui
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = isSystemInDarkTheme()
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !useDarkIcons
                )
            }

            // System Bars
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding()
                )
            }

            val mainNavController = rememberAnimatedNavController()
            val mainNavHostEngine = rememberAnimatedNavHostEngine(
                navHostContentAlignment = Alignment.TopCenter,
                rootDefaultAnimations = RootNavGraphDefaultAnimations(
//                    enterTransition = { slideInHorizontally { it }},
//                    exitTransition = { slideOutHorizontally { -it }},
//                    popEnterTransition = { slideInHorizontally { -it }},
//                    popExitTransition = { slideOutHorizontally { it }},
                    enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
                    exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
                    popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 1.1f) },
                    popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 0.9f) }
                ),
                defaultAnimationsForNestedNavGraph = mapOf()
            )
            // Shared ViewModel
//            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>(this)

            // Screen Sizes
            val sizeClass = calculateWindowSizeClass(activity = this)
//            val shouldShowNav = menuNavController.appCurrentDestinationAsState().value in listOf(
//                MessagePageDestination,
//                FilePageDestination,
//                SettingPageDestination
//            )
            AppTheme {
                CompositionLocalProvider(values = arrayOf(LocalFixedInsets provides fixedInsets)) {
                    DestinationsNavHost(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        navGraph = NavGraphs.root,
                        engine = mainNavHostEngine,
                        navController = mainNavController,
                        dependenciesContainerBuilder = {
                            dependency(mainSharedViewModel)
                            dependency(sizeClass)
                            dependency { event: ActivityEvent -> onEvent(event) }
                        })

                }

//                MessagePage(
//                    onConnectBtnClicked = {
//                        pluginConn.connect()
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                pluginConn.connectionStateFlow.collect {
//                                    Log.d("parabox", "connection state received")
//                                    viewModel.setSendAvailableState(it)
//                                }
//                            }
//                        }
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                repeatOnLifecycle(Lifecycle.State.STARTED) {
//                                    pluginConn.messageResFlow.collect {
//                                        Log.d("parabox", "message received")
//                                        viewModel.setMessage(it)
//                                    }
//                                }
//                            }
//                        }
//                    },
//                    onSendBtnClicked = {
//                        pluginConn.send(
//                            (0..10).random().toString()
//                        )
//                    }
//                )
            }
        }
    }

    override fun onStart() {
        inBackground = false
        lifecycleScope.launch(Dispatchers.Main) {
            val enableFcm = dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
            val fcmRole = dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_ROLE]
                ?: FcmConstants.Role.SENDER.ordinal
            if (!enableFcm || fcmRole == FcmConstants.Role.SENDER.ordinal) {
                val pluginServiceBinderIntent = Intent(this@MainActivity, PluginService::class.java)
                pluginServiceConnection = object : ServiceConnection {
                    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                        Log.d("parabox", "mainActivity - service connected")
                        pluginService =
                            (p1 as PluginService.PluginServiceBinder).getService().also {
                                mainSharedViewModel.setPluginListStateFlow(it.getAppModelList())
                                it.setPluginListListener(object : PluginListListener {
                                    override fun onPluginListChange(pluginList: List<AppModel>) {
                                        mainSharedViewModel.setPluginListStateFlow(pluginList)
                                    }
                                })
                            }
                    }

                    override fun onServiceDisconnected(p0: ComponentName?) {
                        Log.d("parabox", "mainActivity - service disconnected")
                        pluginService = null
                    }

                }
                startService(pluginServiceBinderIntent)
                bindService(pluginServiceBinderIntent, pluginServiceConnection, BIND_AUTO_CREATE)

            }
        }
        super.onStart()
    }

    override fun onStop() {
        inBackground = true
        lifecycleScope.launch(Dispatchers.Main) {
            val enableFcm = dataStore.data.first()[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
            val fcmRole = dataStore.data.first()[DataStoreKeys.SETTINGS_FCM_ROLE]
                ?: FcmConstants.Role.SENDER.ordinal
            if (!enableFcm || fcmRole == FcmConstants.Role.SENDER.ordinal) {
                unbindService(pluginServiceConnection)
                pluginService = null
            }
        }
        super.onStop()
    }
}