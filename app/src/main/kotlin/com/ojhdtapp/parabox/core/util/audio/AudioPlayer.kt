package com.ojhdtapp.parabox.core.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.ojhdtapp.parabox.core.util.MediaResourceResolver
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaResourceResolver: MediaResourceResolver,
) {
    enum class PlayStatus {
        Idle,
        Loading,
        Playing,
        Paused,
        Error,
    }

    data class AudioPlaybackState(
        val activeKey: String? = null,
        val status: PlayStatus = PlayStatus.Idle,
        val positionMs: Int = 0,
        val durationMs: Int = 0,
        val errorMessage: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var prepared: Boolean = false
    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var playGeneration: Long = 0L
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                scope.launch { pause() }
            }
        }
    }

    fun togglePlayPause(
        itemKey: String,
        resourceInfo: ParaboxResourceInfo,
        fallbackDurationSec: Long,
        fileName: String? = null,
    ) {
        scope.launch {
            val current = _playbackState.value
            if (current.activeKey == itemKey) {
                when (current.status) {
                    PlayStatus.Playing -> {
                        pauseInternal()
                        return@launch
                    }
                    PlayStatus.Paused -> {
                        if (prepared && mediaPlayer != null) {
                            resumeInternal()
                            return@launch
                        }
                    }
                    PlayStatus.Loading -> {
                        stopInternal()
                        return@launch
                    }
                    PlayStatus.Idle, PlayStatus.Error -> Unit
                }
            }
            startNewPlayback(
                itemKey = itemKey,
                resourceInfo = resourceInfo,
                fallbackDurationSec = fallbackDurationSec,
                fileName = fileName,
                initialSeekMs = 0,
            )
        }
    }

    fun seekTo(
        itemKey: String,
        positionMs: Int,
        resourceInfo: ParaboxResourceInfo,
        fallbackDurationSec: Long,
        fileName: String? = null,
    ) {
        scope.launch {
            val current = _playbackState.value
            if (current.activeKey == itemKey && prepared && mediaPlayer != null) {
                val player = mediaPlayer ?: return@launch
                val totalMs = player.duration.takeIf { it > 0 } ?: current.durationMs
                val clamped = if (totalMs > 0) positionMs.coerceIn(0, totalMs) else positionMs.coerceAtLeast(0)
                runCatching { player.seekTo(clamped) }
                _playbackState.value = current.copy(
                    positionMs = clamped,
                    durationMs = totalMs,
                )
            } else {
                startNewPlayback(
                    itemKey = itemKey,
                    resourceInfo = resourceInfo,
                    fallbackDurationSec = fallbackDurationSec,
                    fileName = fileName,
                    initialSeekMs = positionMs.coerceAtLeast(0),
                )
            }
        }
    }

    fun pause() {
        scope.launch { pauseInternal() }
    }

    fun stop() {
        scope.launch { stopInternal() }
    }

    private fun startNewPlayback(
        itemKey: String,
        resourceInfo: ParaboxResourceInfo,
        fallbackDurationSec: Long,
        fileName: String?,
        initialSeekMs: Int,
    ) {
        releasePlayerOnly()
        val generation = ++playGeneration
        val fallbackDurationMs = (fallbackDurationSec.coerceAtLeast(0L) * 1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        _playbackState.value = AudioPlaybackState(
            activeKey = itemKey,
            status = PlayStatus.Loading,
            positionMs = initialSeekMs.coerceAtMost(fallbackDurationMs.takeIf { it > 0 } ?: Int.MAX_VALUE),
            durationMs = fallbackDurationMs,
        )

        loadJob = scope.launch {
            val uriResult = mediaResourceResolver.resolveAudioUri(
                resourceInfo = resourceInfo,
                suggestedFileName = fileName,
            )
            if (generation != playGeneration) return@launch

            val uri = uriResult.getOrElse { err ->
                _playbackState.value = AudioPlaybackState(
                    activeKey = itemKey,
                    status = PlayStatus.Error,
                    positionMs = 0,
                    durationMs = fallbackDurationMs,
                    errorMessage = err.message ?: "音频加载失败",
                )
                return@launch
            }

            prepareAndPlay(
                generation = generation,
                itemKey = itemKey,
                uri = uri,
                fallbackDurationMs = fallbackDurationMs,
                initialSeekMs = initialSeekMs,
            )
        }
    }

    private fun prepareAndPlay(
        generation: Long,
        itemKey: String,
        uri: Uri,
        fallbackDurationMs: Int,
        initialSeekMs: Int,
    ) {
        val player = MediaPlayer()
        mediaPlayer = player
        prepared = false

        try {
            player.setAudioAttributes(audioAttributes)
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                player.setDataSource(uri.path)
            } else {
                player.setDataSource(context, uri)
            }

            player.setOnPreparedListener { mp ->
                if (generation != playGeneration || mp !== mediaPlayer) {
                    runCatching { mp.release() }
                    return@setOnPreparedListener
                }
                prepared = true
                val realDurationMs = mp.duration.takeIf { it > 0 } ?: fallbackDurationMs
                val startPos = if (realDurationMs > 0) {
                    initialSeekMs.coerceIn(0, realDurationMs)
                } else {
                    0
                }
                if (startPos > 0) {
                    runCatching { mp.seekTo(startPos) }
                }
                requestAudioFocus()
                mp.start()
                _playbackState.value = AudioPlaybackState(
                    activeKey = itemKey,
                    status = PlayStatus.Playing,
                    positionMs = startPos,
                    durationMs = realDurationMs,
                )
                startProgressLoop(generation, itemKey, realDurationMs)
            }

            player.setOnCompletionListener { mp ->
                if (generation != playGeneration || mp !== mediaPlayer) return@setOnCompletionListener
                progressJob?.cancel()
                progressJob = null
                abandonAudioFocus()
                val duration = mp.duration.takeIf { it > 0 } ?: fallbackDurationMs
                runCatching { mp.seekTo(0) }
                _playbackState.value = AudioPlaybackState(
                    activeKey = itemKey,
                    status = PlayStatus.Idle,
                    positionMs = 0,
                    durationMs = duration,
                )
            }

            player.setOnErrorListener { mp, what, extra ->
                if (generation != playGeneration || mp !== mediaPlayer) {
                    runCatching { mp.release() }
                    return@setOnErrorListener true
                }
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra uri=$uri")
                releasePlayerOnly()
                _playbackState.value = AudioPlaybackState(
                    activeKey = itemKey,
                    status = PlayStatus.Error,
                    positionMs = 0,
                    durationMs = fallbackDurationMs,
                    errorMessage = "无法播放此音频",
                )
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to prepare MediaPlayer for uri=$uri", e)
            releasePlayerOnly()
            _playbackState.value = AudioPlaybackState(
                activeKey = itemKey,
                status = PlayStatus.Error,
                positionMs = 0,
                durationMs = fallbackDurationMs,
                errorMessage = "音频格式不受支持",
            )
        }
    }

    private fun pauseInternal() {
        val player = mediaPlayer ?: return
        val current = _playbackState.value
        if (prepared && runCatching { player.isPlaying }.getOrDefault(false)) {
            progressJob?.cancel()
            progressJob = null
            runCatching { player.pause() }
            abandonAudioFocus()
            val pos = runCatching { player.currentPosition }.getOrDefault(current.positionMs)
            val dur = runCatching { player.duration }.getOrDefault(current.durationMs)
                .takeIf { it > 0 } ?: current.durationMs
            _playbackState.value = current.copy(
                status = PlayStatus.Paused,
                positionMs = pos.coerceAtLeast(0),
                durationMs = dur,
            )
        }
    }

    private fun resumeInternal() {
        val player = mediaPlayer ?: return
        val current = _playbackState.value
        val activeKey = current.activeKey ?: return
        if (!prepared) return
        requestAudioFocus()
        runCatching { player.start() }
        val dur = runCatching { player.duration }.getOrDefault(current.durationMs)
            .takeIf { it > 0 } ?: current.durationMs
        val pos = runCatching { player.currentPosition }.getOrDefault(current.positionMs)
        _playbackState.value = current.copy(
            status = PlayStatus.Playing,
            positionMs = pos.coerceAtLeast(0),
            durationMs = dur,
        )
        startProgressLoop(playGeneration, activeKey, dur)
    }

    private fun stopInternal() {
        playGeneration++
        releasePlayerOnly()
        _playbackState.value = AudioPlaybackState()
    }

    private fun startProgressLoop(generation: Long, itemKey: String, fallbackDurationMs: Int) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && generation == playGeneration) {
                val player = mediaPlayer ?: break
                val isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
                if (!isPlaying) break
                val pos = runCatching { player.currentPosition }.getOrDefault(0).coerceAtLeast(0)
                val dur = runCatching { player.duration }.getOrDefault(fallbackDurationMs)
                    .takeIf { it > 0 } ?: fallbackDurationMs
                _playbackState.value = AudioPlaybackState(
                    activeKey = itemKey,
                    status = PlayStatus.Playing,
                    positionMs = if (dur > 0) pos.coerceIn(0, dur) else pos,
                    durationMs = dur,
                )
                delay(PROGRESS_POLL_MS)
            }
        }
    }

    private fun releasePlayerOnly() {
        loadJob?.cancel()
        loadJob = null
        progressJob?.cancel()
        progressJob = null
        prepared = false
        mediaPlayer?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        audioFocusRequest = request
        runCatching { manager.requestAudioFocus(request) }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        audioFocusRequest?.let { req ->
            runCatching { manager.abandonAudioFocusRequest(req) }
        }
        audioFocusRequest = null
    }

    companion object {
        private const val TAG = "AudioPlayer"
        private const val PROGRESS_POLL_MS = 50L
    }
}

val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayer> {
    error("no audio player provided")
}
