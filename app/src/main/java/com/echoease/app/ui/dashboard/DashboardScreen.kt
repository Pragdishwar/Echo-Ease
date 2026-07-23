package com.echoease.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
            when (state) {
                is DashboardState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DashboardState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((state as DashboardState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is DashboardState.Success -> {
                    val successState = state as DashboardState.Success
                    DashboardContent(
                        incidents = successState.incidents,
                        strikeLevel = successState.strikeLevel
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    incidents: List<ConfirmedIncident>,
    strikeLevel: Int
) {
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
            Text(
                text = "History (30-Day Window)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (incidents.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No incidents on record. Your space is tranquil!",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(incidents) { incident ->
                IncidentItem(incident)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
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
fun IncidentItem(incident: ConfirmedIncident) {
    val sdf = SimpleDateFormat("EEEE, MMM dd • hh:mm a", Locale.getDefault())
    ListItem(
        headlineContent = { 
            Text(
                "Confirmed Disturbance", 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold 
            ) 
        },
        supportingContent = { 
            Text(
                sdf.format(Date(incident.timestamp)),
                style = MaterialTheme.typography.bodySmall 
            ) 
        },
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    Icons.Default.Warning, 
                    null, 
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
        }
    )
}
