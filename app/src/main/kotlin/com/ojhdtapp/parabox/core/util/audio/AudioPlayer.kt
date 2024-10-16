package com.ojhdtapp.parabox.core.util.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.os.IResultReceiver.Stub
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayer(
    private val context: Context
) {
    private var player: MediaPlayer? = null

    private var statusJob: Job? = null
    private var status : MutableStateFlow<Status>? = null

    suspend fun play(uri: Uri): StateFlow<Status> {
        try {
            MediaPlayer.create(context, uri).apply {
                player = this
                start()
            }
            updateStatusWhenPlaying()
            status = MutableStateFlow(Status.Playing(0, 0))
            return status!!.asStateFlow()
        } catch (e: Exception) {
            e.printStackTrace()
            return MutableStateFlow(Status.Error(0, 0))
        }
    }

    fun pause() {
        if (player?.isPlaying == true) {
            statusJob?.cancel()
            statusJob = null
            player?.pause()
            status?.value = Status.Pause(player?.currentPosition ?: 0, player?.duration ?: 0)
        }
    }

    suspend fun resume() {
        if (player?.isPlaying == false) {
            player?.start()
            updateStatusWhenPlaying()
        }
    }

    fun stop() {
        statusJob?.cancel()
        statusJob = null
        player?.stop()
        player?.release()
        player = null
        status?.value = Status.Pause(player?.currentPosition ?: 0, player?.duration ?: 0)
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    private suspend fun updateStatusWhenPlaying() {
        coroutineScope {
            statusJob = launch(Dispatchers.IO) {
                while (player?.isPlaying == true) {
                    status?.value = Status.Playing(player?.currentPosition ?: 0, player?.duration ?: 0)
                    delay(500)
                }
                cancel()
            }
        }
    }

    sealed interface Status {
        val position: Int
        val duration: Int
        data class Playing(
            override val position: Int,
            override val duration: Int
        ) : Status
        data class Pause(
            override val position: Int,
            override val duration: Int
        ): Status
        data class Error(
            override val position: Int,
            override val duration: Int
        ) : Status
    }
}

val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayer> {
    error("no audio player provided")
}