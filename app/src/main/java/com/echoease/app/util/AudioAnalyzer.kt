package com.echoease.app.util

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

class AudioAnalyzer {
    private val _decibels = MutableStateFlow(0.0)
    val decibels = _decibels.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()

        job = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / read)
                    // Convert RMS to dB (reference 1.0)
                    // Note: This is an approximation for mobile mics
                    val db = if (rms > 0) 20 * log10(rms) else 0.0
                    _decibels.value = db
                }
                delay(100) // Update every 100ms
            }
        }
    }

    fun stop() {
        job?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
