package com.echoease.app.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null

    fun startRecording() {
        audioFile = File(context.cacheDir, "noise_sample.m4a")
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)

            try {
                prepare()
                start()
                Log.d("AudioRecorder", "Recording started")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed: ${e.message}")
        }
        recorder = null
    }

    fun playSample() {
        player = MediaPlayer().apply {
            try {
                setDataSource(audioFile?.absolutePath)
                prepare()
                start()
                Log.d("AudioRecorder", "Playback started")
            } catch (e: IOException) {
                Log.e("AudioRecorder", "play failed: ${e.message}")
            }
        }
    }

    fun stopPlayback() {
        player?.release()
        player = null
    }

    fun cleanup() {
        stopRecording()
        stopPlayback()
        audioFile?.delete()
    }

    fun getFile(): File? = audioFile
}
