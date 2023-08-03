package com.ojhdtapp.parabox.ui.message.chat

import com.ojhdtapp.parabox.R

sealed class AudioRecorderState(
    val textResId: Int
){
    object Ready : AudioRecorderState(R.string.audio_recorder_state_text_ready)
    object Recording: AudioRecorderState(R.string.audio_recorder_state_text_recording)
    object Confirmed: AudioRecorderState(R.string.audio_recorder_state_text_confirmed)
    object Done : AudioRecorderState(R.string.audio_recorder_state_text_done)
}