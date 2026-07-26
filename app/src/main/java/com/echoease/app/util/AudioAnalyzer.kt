package com.echoease.app.util

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
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
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        
        // Try multiple buffer sizes to find the most compatible one
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = if (minBufferSize > 0) minBufferSize * 2 else 4096

        try {
            // Stop any existing session
            stop()

            val sources = listOf(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.CAMCORDER
            )

            var initialized = false
            for (source in sources) {
                if (initialized) break
                
                try {
                    audioRecord = AudioRecord(
                        source,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                    
                    if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                        initialized = true
                        Log.d("AudioAnalyzer", "Microphone started with source: $source")
                    } else {
                        audioRecord?.release()
                    }
                } catch (e: Exception) {
                    Log.e("AudioAnalyzer", "Failed to init source $source: ${e.message}")
                }
            }

            if (initialized) {
                audioRecord?.startRecording()
                Log.d("AudioAnalyzer", "Microphone recording state: ${audioRecord?.recordingState}")

                job = scope.launch(Dispatchers.Default) {
                    val buffer = ShortArray(bufferSize)
                    while (isActive) {
                        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            // Read in small chunks for higher framerate (~43fps) instead of waiting for full buffer
                            val read = audioRecord?.read(buffer, 0, minOf(buffer.size, 1024)) ?: 0
                            if (read > 0) {
                                var sum = 0.0
                                var maxAmplitude = 0
                                for (i in 0 until read) {
                                    val value = buffer[i].toDouble()
                                    sum += value * value
                                    if (Math.abs(buffer[i].toInt()) > maxAmplitude) {
                                        maxAmplitude = Math.abs(buffer[i].toInt())
                                    }
                                }
                                
                                val rms = sqrt(sum / read)
                                var db = if (rms > 0.0) {
                                    val dbfs = 20 * log10(rms / 32768.0)
                                    (dbfs + 110.0).coerceAtLeast(0.0)
                                } else {
                                    0.0 // Real absolute silence
                                }

                                val targetDb = db.coerceIn(0.0, 115.0)
                                val currentDb = _decibels.value
                                _decibels.value = if (currentDb <= 0.0) targetDb else (currentDb * 0.4) + (targetDb * 0.6)
                            } else {
                                // Microphone failed to read real data
                                _decibels.value = 0.0
                                delay(50) 
                            }
                        } else {
                            // Hardware failed to start
                            _decibels.value = 0.0
                            delay(50) 
                        }
                    }
                }
            } else {
                Log.e("AudioAnalyzer", "Could not initialize ANY Microphone source")
                _decibels.value = -1.0 // Indicate error
            }
        } catch (e: Exception) {
            Log.e("AudioAnalyzer", "Microphone Error: ${e.message}")
        }
    }

    fun stop() {
        job?.cancel()
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioAnalyzer", "Cleanup Error: ${e.message}")
        }
        audioRecord = null
    }

    fun setManualDb(db: Double) {
        val targetDb = db.coerceIn(0.0, 115.0)
        val currentDb = _decibels.value
        _decibels.value = if (currentDb <= 0.0) targetDb else (currentDb * 0.4) + (targetDb * 0.6)
    }
}
