package com.echoease.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun LiveWaveform(
    db: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val color = MaterialTheme.colorScheme.primary
    val amplitude = (db.coerceIn(30.0, 100.0) - 30.0).toFloat() * 3f

    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val points = 50
        val step = width / points

        for (i in 0 until points) {
            val x = i * step
            val variation = sin(phase + i * 0.5f) * amplitude
            val lineheight = (10f + variation).coerceAtLeast(4f)
            
            drawLine(
                color = color,
                start = Offset(x, centerY - lineheight / 2),
                end = Offset(x, centerY + lineheight / 2),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
