package com.echoease.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.data.model.BuildingConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: AdminViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Building Management") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                is AdminState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AdminState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((state as AdminState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is AdminState.Success -> {
                    AdminContent(
                        config = (state as AdminState.Success).config,
                        onSave = { viewModel.updateConfig(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminContent(
    config: BuildingConfig,
    onSave: (BuildingConfig) -> Unit
) {
    var threshold by remember { mutableIntStateOf(config.consensusThreshold) }
    var wardenThreshold by remember { mutableIntStateOf(config.escalationTiers.lastOrNull() ?: 4) }
    var wardenContact by remember { mutableStateOf(config.wardenContact) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mediation Tuning",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Consensus Threshold
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Consensus Threshold", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Number of independent flags required to confirm an incident.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { threshold = it.toInt() },
                        valueRange = 2f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = threshold.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Escalation Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Warden Escalation", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Loop in management after this many strikes.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = wardenThreshold.toFloat(),
                        onValueChange = { wardenThreshold = it.toInt() },
                        valueRange = 3f..10f,
                        steps = 7,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = wardenThreshold.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = wardenContact,
                    onValueChange = { wardenContact = it },
                    label = { Text("Warden Contact (Email/Phone)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                val newConfig = config.copy(
                    consensusThreshold = threshold,
                    escalationTiers = listOf(2, 3, wardenThreshold),
                    wardenContact = wardenContact
                )
                onSave(newConfig)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE CONFIGURATION")
        }
    }
}
