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
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.smartreply.*
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.LanguageUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.service.PluginListListener
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.domain.use_case.GetContacts
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.roundToInt

@AndroidEntryPoint
class BubbleActivity : AppCompatActivity() {
    @Inject
    lateinit var handleNewMessage: HandleNewMessage

    @Inject
    lateinit var getContacts: GetContacts

    @Inject
    lateinit var appDatabase: AppDatabase

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

    // ML
    private var entityExtractor: EntityExtractor? = null
    private var smartReplyGenerator: SmartReplyGenerator? = null

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
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
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

    // ML
    suspend fun getEntityAnnotationList(str: String): List<EntityAnnotation> {
        return suspendCoroutine<List<EntityAnnotation>> { cot ->
            Log.d("parabox", "getEntityAnnotationList: $str")
            if (entityExtractor == null) cot.resume(emptyList<EntityAnnotation>())
            else {
                val params = EntityExtractionParams.Builder(str).build()
                entityExtractor!!.annotate(params)
                    .addOnSuccessListener { result ->
                        cot.resume(result)
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        cot.resumeWithException(it)
                    }
            }
        }
    }

    suspend fun getSmartReplyList(contactId: Long): List<SmartReplySuggestion> {
        Log.d("parabox", "getSmartReplyList: $contactId")
        if (smartReplyGenerator == null) return emptyList()
        val conversation = withContext(Dispatchers.IO) {
            appDatabase.messageDao.getMessagesWithLimit(listOf(contactId), 3)
                .sortedBy { it.timestamp }.map {
                    if (it.sentByMe) {
                        TextMessage.createForLocalUser(it.contentString, it.timestamp)
                    } else {
                        TextMessage.createForRemoteUser(
                            it.contentString,
                            it.timestamp,
                            it.profile.name
                        )
                    }
                }
        }
        return suspendCoroutine<List<SmartReplySuggestion>> { cot ->
            Log.d("parabox", "getSmartReplyList: ${conversation.last().messageText}")
            smartReplyGenerator!!.suggestReplies(conversation)
                .addOnSuccessListener { result ->
                    if (result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                        cot.resume(emptyList())
                    } else if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        if (conversation.lastOrNull()?.isLocalUser == true) {
                            cot.resume(emptyList())
                        } else {
                            cot.resume(result.suggestions)
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    cot.resumeWithException(it)
                }
        }
    }

    suspend fun getTranslation(originalText: String): String? {
        return try {
            val languageCode = getLanguageCode(originalText)
            val currentLanguageTag =
                AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: "en"
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode)!!)
                .setTargetLanguage(
                    TranslateLanguage.fromLanguageTag(
                        LanguageUtil.languageTagMapper(currentLanguageTag)
                    )!!
                )
                .build()
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            return suspendCoroutine { cot ->
                val translator = Translation.getClient(options)
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.d("parabox", "downloadModelIfNeeded: success")
                        translator.translate(originalText)
                            .addOnSuccessListener { translatedText ->
                                Log.d("parabox", "translated: $translatedText")
                                cot.resume(translatedText)
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                                cot.resumeWithException(it)
                            }.addOnCompleteListener {
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        Log.d("parabox", "downloadModelIfNeeded: failed")
                        cot.resumeWithException(it)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLanguageCode(str: String): String {
        return suspendCoroutine<String> { cot ->
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(str)
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        cot.resume("en")
                    } else {
                        cot.resume(languageCode)
                    }
                }
                .addOnFailureListener {
                    cot.resume("en")
                }
        }
    }

    private fun initializeMLKit() {
        lifecycleScope.launch {
            val isEntityExtractionEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_ENTITY_EXTRACTION] ?: true
            val isSmartReplyEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_SMART_REPLY] ?: true
            val isTranslationEnabled =
                dataStore.data.first()[DataStoreKeys.SETTINGS_ML_KIT_TRANSLATION] ?: true
            if (isEntityExtractionEnabled) {
                val tempEntityExtractor =
                    EntityExtraction.getClient(
                        EntityExtractorOptions.Builder(
                            AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.let {
                                EntityExtractorOptions.fromLanguageTag(it)
                            } ?: EntityExtractorOptions.ENGLISH
                        ).build())
                tempEntityExtractor
                    .downloadModelIfNeeded()
                    .addOnSuccessListener { _ ->
                        entityExtractor = tempEntityExtractor
                        lifecycle.addObserver(entityExtractor!!)
                    }
            }
            if (isSmartReplyEnabled) {
                smartReplyGenerator = SmartReply.getClient()
            }
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
                    Toast.makeText(this, getString(R.string.toast_playing_media_stop), Toast.LENGTH_SHORT).show()
                }
                startRecording()
            }

            is ActivityEvent.StopRecording -> {
                stopRecording()
            }

            is ActivityEvent.StartAudioPlaying -> {
                if (recorder != null) {
                    Toast.makeText(this, getString(R.string.toast_stop_recording_first), Toast.LENGTH_SHORT).show()
                } else {
                    if (event.uri != null) {
                        startPlayingLocal(event.uri)
                    } else if (event.url != null) {
                        startPlayingInternet(event.url)
                    } else {
                        Toast.makeText(this, getString(R.string.media_res_lost), Toast.LENGTH_SHORT).show()
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
            is ActivityEvent.LaunchApp -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load Message
        val id = intent.data?.lastPathSegment?.toLongOrNull() ?: return
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    getContacts.queryById(id)
                }.also {
                    if (it != null) {
                        viewModel.loadMessageFromContact(it)
                    }
                }
            }
        }


        // Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Record
        recordPath = "${externalCacheDir!!.absoluteFile}/audio_record.mp3"

        // ML
        initializeMLKit()

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