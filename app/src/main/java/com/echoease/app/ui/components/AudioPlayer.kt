package com.echoease.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AudioPlayer(
    audioUrl: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isError by remember { mutableStateOf(false) }

    DisposableEffect(audioUrl) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(audioUrl)
                setOnPreparedListener { 
                    isPrepared = true
                    duration = it.duration
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, _, _ ->
                    isError = true
                    false
                }
                prepareAsync()
            } catch (e: Exception) {
                isError = true
            }
        }
        mediaPlayer = player

        onDispose {
            if (player.isPlaying) player.stop()
            player.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition
                }
            }
            delay(100)
        }
    }

    if (isError) {
        Text("Audio unavailable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                mediaPlayer?.let {
                    if (isPlaying) {
                        it.pause()
                        isPlaying = false
                    } else if (isPrepared) {
                        it.start()
                        isPlaying = true
                    }
                }
            },
            enabled = isPrepared,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = {
                mediaPlayer?.let {
                    if (isPlaying || currentPosition > 0) {
                        it.pause()
                        it.seekTo(0)
                        currentPosition = 0
                        isPlaying = false
                    }
                }
            },
            enabled = isPrepared && (isPlaying || currentPosition > 0),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                tint = MaterialTheme.colorScheme.error
            )
        }

        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )

        val formatTime = { ms: Int ->
            val seconds = (ms / 1000) % 60
            val minutes = (ms / 1000) / 60
            String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
        
        Text(
            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
