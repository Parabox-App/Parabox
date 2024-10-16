package com.ojhdtapp.parabox.core.util.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

object AudioRecorder {
    private var recorder: MediaRecorder? = null

    private fun createRecorder(context: Context): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start(context: Context, outputFile: File) {
        createRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)

            prepare()
            start()

            recorder = this
        }
    }

    fun stop(): Boolean {
        return try {
            recorder?.stop()
            recorder?.reset()
            recorder = null
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }
}

val LocalAudioRecorder = staticCompositionLocalOf<AudioRecorder> {
    AudioRecorder
}