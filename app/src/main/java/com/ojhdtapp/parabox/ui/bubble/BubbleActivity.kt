package com.ojhdtapp.parabox.ui.bubble

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.service.PluginListListener
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FixedInsets
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.IOException
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@AndroidEntryPoint
class BubbleActivity : AppCompatActivity() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    var pluginService: PluginService? = null
    private lateinit var pluginServiceConnection: ServiceConnection

    private val viewModel by viewModels<BubblePageViewModel>()

    private var recorder: MediaRecorder? = null
    private var recorderJob: Job? = null
    private var recorderStartTime: Long? = null
    private lateinit var recordPath: String
    private var player: MediaPlayer? = null
    private var playerJob: Job? = null
    private var amplituda: Amplituda? = null

    lateinit var vibrator: Vibrator

    private fun startPlayingLocal(uri: Uri) {
        stopPlaying()
        player = MediaPlayer().apply {
            try {
                setDataSource(applicationContext, uri)
                setOnPreparedListener {
                    playerJob = lifecycleScope.launch {
                        while (true) {
                            val progress = (currentPosition.toFloat() / duration)
                            viewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(this@BubbleActivity).also { amplituda ->
                        amplituda.processAudio(
                            FileUtil.uriToTempFile(this@BubbleActivity, uri),
                            Compress.withParams(Compress.AVERAGE, 2)
                        ).get(
                            { result ->
                                viewModel.insertAllIntoRecordAmplitudeStateList(
                                    result.amplitudesAsList().map { it * 1000 })
                            }, { exception ->
                                exception.printStackTrace()
                            })
                    }
                    viewModel.setIsAudioPlaying(true)
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    viewModel.clearRecordAmplitudeStateList()
                    viewModel.setIsAudioPlaying(false)
                }
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
        }
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
                            viewModel.setAudioPlayerProgressFraction(progress)
                            delay(30)
                        }
                    }
                    amplituda = Amplituda(this@BubbleActivity).also { amplituda ->
                        amplituda.processAudio(
                            url,
                            Compress.withParams(Compress.AVERAGE, 2)
                        ).get(
                            { result ->
                                viewModel.insertAllIntoRecordAmplitudeStateList(
                                    result.amplitudesAsList().map { it * 1000 })
                            }, { exception ->
                                exception.printStackTrace()
                            })
                    }
                    viewModel.setIsAudioPlaying(true)
                }
                setOnCompletionListener {
                    amplituda?.clearCache()
                    amplituda = null
                    playerJob?.cancel()
                    playerJob = null
                    viewModel.clearRecordAmplitudeStateList()
                    viewModel.setIsAudioPlaying(false)
                }
                prepareAsync()
            } catch (e: IOException) {
                Log.e("parabox", "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        playerJob?.cancel()
        playerJob = null
        player?.release()
        player = null
        viewModel.setIsAudioPlaying(false)
    }

    private fun pausePlaying() {
        if (player?.isPlaying == true) {
            player?.pause()
            viewModel.setIsAudioPlaying(false)
        }
    }

    private fun resumePlaying() {
        if (player?.isPlaying == false) {
            player?.start()
            viewModel.setIsAudioPlaying(true)
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
        viewModel.clearRecordAmplitudeStateList()
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
                viewModel.insertIntoRecordAmplitudeStateList(value)
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

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 20))
        }
    }

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
            
            is ActivityEvent.Vibrate -> {
                vibrate()
            }
        }
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load Message
        intent.getParcelableExtra<Contact>("contact")?.let {
            Log.d("parabox", "contact loaded: $it")
            viewModel.loadMessageFromContact(it)
        }

        // Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Record
        recordPath = "${externalCacheDir!!.absoluteFile}/audio_record.mp3"

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent{
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

            // Screen Sizes
            val sizeClass = calculateWindowSizeClass(activity = this)
            AppTheme {
                CompositionLocalProvider(values = arrayOf(LocalFixedInsets provides fixedInsets)) {
                    BubblePage(
                        viewModel = viewModel,
                        sizeClass = sizeClass,
                        onEvent = ::onEvent,
                    )
                }
            }
        }
    }

    override fun onStart() {
        val pluginServiceBinderIntent = Intent(this, PluginService::class.java)
        pluginServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                pluginService = (p1 as PluginService.PluginServiceBinder).getService().also {
                    viewModel.setPluginListStateFlow(it.getAppModelList())
                    it.setPluginListListener(object : PluginListListener {
                        override fun onPluginListChange(pluginList: List<AppModel>) {
                            viewModel.setPluginListStateFlow(pluginList)
                        }
                    })
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                pluginService = null
            }

        }
        startService(pluginServiceBinderIntent)
        bindService(pluginServiceBinderIntent, pluginServiceConnection, BIND_AUTO_CREATE)
        super.onStart()
    }

    override fun onStop() {
        unbindService(pluginServiceConnection)
        pluginService = null
        super.onStop()
    }
}