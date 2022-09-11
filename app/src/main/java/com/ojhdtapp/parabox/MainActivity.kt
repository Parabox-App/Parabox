package com.ojhdtapp.parabox

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FixedInsets
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage
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

    // Shared ViewModel
    val mainSharedViewModel by viewModels<MainSharedViewModel>()


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
                Log.e("parabox", "prepare() failed")
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
        recorder = MediaRecorder().apply {
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

    // Event
    fun onEvent(event: ActivityEvent) {
        when (event) {
            is ActivityEvent.LaunchIntent -> {
                startActivity(event.intent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
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
                        pluginService?.sendMessage(dto)
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
                Log.d("parabox", event.uri.toString())
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
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // Receive Status Flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(2000)
                Log.d("parabox", "isPluginServiceNull: ${pluginService == null}")
                pluginService?.getPluginListFlow()?.collectLatest {
                    mainSharedViewModel.setPluginListStateFlow(it)
                }
            }
        }

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
                    popEnterTransition = { fadeIn(tween(450)) + scaleIn(tween(450), 1.1f) },
                    popExitTransition = { fadeOut(tween(450)) + scaleOut(tween(450), 0.9f) }
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
        val pluginServiceBinderIntent = Intent(this, PluginService::class.java)
        pluginServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                Log.d("parabox", "mainActivity - service connected")
                pluginService = (p1 as PluginService.PluginServiceBinder).getService()
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                Log.d("parabox", "mainActivity - service disconnected")
                pluginService = null
            }

        }
        startService(pluginServiceBinderIntent)
        bindService(pluginServiceBinderIntent, pluginServiceConnection, BIND_AUTO_CREATE)
        super.onStart()
    }

    override fun onStop() {
        if (pluginService != null) {
            unbindService(pluginServiceConnection)
            pluginService = null
        }
        super.onStop()
    }
}