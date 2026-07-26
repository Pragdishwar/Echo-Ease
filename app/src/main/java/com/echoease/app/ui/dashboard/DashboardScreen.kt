package com.echoease.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.data.model.ConfirmedIncident
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val mediaPlayer = remember { android.media.MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Room Intelligence") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            androidx.compose.animation.AnimatedContent(
                targetState = state,
                label = "DashboardStateAnimation"
            ) { targetState ->
                when (targetState) {
                    is DashboardState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DashboardState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(), 
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(targetState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadDashboard() }) {
                                Text("Retry")
                            }
                        }
                    }
                    is DashboardState.Success -> {
                        DashboardContent(
                            myIncidents = targetState.myIncidents,
                            flaggedByMe = targetState.flaggedByMe,
                            strikeLevel = targetState.strikeLevel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    myIncidents: List<ConfirmedIncident>,
    flaggedByMe: List<ConfirmedIncident>,
    strikeLevel: Int
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val (tone, color) = when {
        strikeLevel <= 1 -> "Quiet & Peaceful. You're doing great!" to MaterialTheme.colorScheme.primary
        strikeLevel == 2 -> "Awareness: Neighbors have noticed occasional noise." to Color(0xFFFBC02D)
        strikeLevel == 3 -> "Alert: Consistent noise patterns confirmed by neighbors." to Color(0xFFF57C00)
        else -> "Critical: Significant noise reports recorded. Action needed." to MaterialTheme.colorScheme.error
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            EscalationLadder(strikeLevel = strikeLevel, activeColor = color)
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = color, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Current Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tone,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Reminders") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reports I Sent") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val displayList = if (selectedTab == 0) myIncidents else flaggedByMe

        if (displayList.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (selectedTab == 0) "No incidents on record. Your space is tranquil!" else "No flags sent recently. Community is quiet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items = displayList, key = { it.timestamp }) { incident ->
                IncidentItem(incident, isSentByMe = selectedTab == 1)
            }
        }
    }
}

@Composable
fun EscalationLadder(strikeLevel: Int, activeColor: Color) {
    val progress = (strikeLevel.coerceAtMost(4) / 4f)
    val animatedProgress by animateFloatAsState(targetValue = progress)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "STRIKE LEVEL",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = strikeLevel.toString(),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = activeColor
        )
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            color = activeColor,
            trackColor = activeColor.copy(alpha = 0.15f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SAFE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("WARNING", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("CRITICAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncidentItem(
    incident: com.echoease.app.data.model.ConfirmedIncident,
    isSentByMe: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(incident.timestamp))
                    Text(if (isSentByMe) "Target: Room ${incident.roomId}" else "Incident Logged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                
                if (incident.severity > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("Severity ${incident.severity}", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(incident.timestamp))
                    Text("Exact Time: $timeStr", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (incident.audioProofUrl != null) {
                        Text("Audio Evidence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        com.echoease.app.ui.components.AudioPlayer(audioUrl = incident.audioProofUrl)
                    } else {
                        Text("No audio evidence attached.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardContentPreview() {
    MaterialTheme {
        DashboardContent(
            myIncidents = listOf(
                ConfirmedIncident(id = "1", timestamp = System.currentTimeMillis() - 86400000),
                ConfirmedIncident(id = "2", timestamp = System.currentTimeMillis() - 172800000, isWardenEscalated = true)
            ),
            flaggedByMe = emptyList(),
            strikeLevel = 2
        )
    }
}
