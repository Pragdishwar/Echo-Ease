package com.echoease.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoease.app.data.model.Room

@Composable
fun FloorHeatmap(
    rooms: List<Room>,
    incidentCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Floor Activity Heatmap",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(rooms) { room ->
                val count = incidentCounts[room.id] ?: 0
                val bgColor = when {
                    count == 0 -> Color(0xFFE8F5E9) // Green (Safe)
                    count <= 2 -> Color(0xFFFFF9C4) // Yellow (Warning)
                    count <= 4 -> Color(0xFFFFCCBC) // Orange (Critical)
                    else -> Color(0xFFFFEBEE) // Red (Warden Needed)
                }
                
                val borderColor = when {
                    count == 0 -> Color(0xFF4CAF50)
                    count <= 2 -> Color(0xFFFBC02D)
                    count <= 4 -> Color(0xFFFF5722)
                    else -> Color(0xFFD32F2F)
                }
                
                val textColor = when {
                    count == 0 -> Color(0xFF2E7D32)
                    count <= 2 -> Color(0xFFF57F17)
                    count <= 4 -> Color(0xFFD84315)
                    else -> Color(0xFFC62828)
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(bgColor, MaterialTheme.shapes.small)
                        .border(1.dp, borderColor, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = room.name ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                        Text(
                            text = "Floor ${room.floor ?: '-'}",
                            style = MaterialTheme.typography.labelExtraSmall.copy(fontSize = 8.sp),
                            color = Color.DarkGray
                        )
                        Text(
                            text = if (count > 0) "$count 🔥" else "Peaceful",
                            style = MaterialTheme.typography.labelExtraSmall.copy(fontSize = 8.sp),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

// Fallback for typography if labelExtraSmall doesn't exist in the project's theme
private val androidx.compose.material3.Typography.labelExtraSmall: androidx.compose.ui.text.TextStyle
    @Composable get() = labelSmall.copy(fontSize = 10.sp)
