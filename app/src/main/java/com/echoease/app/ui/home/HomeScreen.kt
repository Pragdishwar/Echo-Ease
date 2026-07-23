package com.echoease.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import com.echoease.app.util.AudioAnalyzer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAmbientCheck by remember { mutableStateOf(false) }
    
    val audioAnalyzer = remember { AudioAnalyzer() }
    val currentDb by audioAnalyzer.decibels.collectAsState()
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAmbientCheck = true
            audioAnalyzer.start(scope)
        }
    }

    DisposableEffect(showAmbientCheck) {
        onDispose {
            audioAnalyzer.stop()
        }
    }

    LaunchedEffect(state) {
        when (state) {
            is HomeState.Success -> {
                snackbarHostState.showSnackbar("Your report has been submitted anonymously.")
                viewModel.resetState()
            }
            is HomeState.Error -> {
                snackbarHostState.showSnackbar((state as HomeState.Error).message)
                viewModel.resetState()
            }
            is HomeState.RateLimited -> {
                val mins = (state as HomeState.RateLimited).remainingMillis / 60000
                snackbarHostState.showSnackbar("Cooldown active: Please wait $mins minutes.")
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "EchoEase", 
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            Icons.Default.Assessment, 
                            contentDescription = "My Room History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Is it too loud?",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Securely flag disturbances. We'll notify neighbors if the consensus logic confirms the noise source.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            Box(contentAlignment = Alignment.Center) {
                if (state is HomeState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(180.dp),
                        strokeWidth = 12.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LargeFloatingActionButton(
                        onClick = { viewModel.flagNoise() },
                        modifier = Modifier.size(180.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = FloatingActionButtonDefaults.largeShape
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.NotificationsActive, 
                                null, 
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "FLAG NOW", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))

            OutlinedButton(
                onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                            showAmbientCheck = true
                            audioAnalyzer.start(scope)
                        }
                        else -> {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(12.dp))
                Text("AMBIENT CHECK", fontWeight = FontWeight.Bold)
            }

            if (showAmbientCheck) {
                AlertDialog(
                    onDismissRequest = { 
                        showAmbientCheck = false
                        audioAnalyzer.stop()
                    },
                    confirmButton = {
                        Button(onClick = { 
                            showAmbientCheck = false
                            audioAnalyzer.stop()
                        }) { Text("OK") }
                    },
                    title = { Text("Environment Check") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(
                                String.format("%.1f dB", currentDb), 
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                if (currentDb < 60) "Ambient level is currently SAFE." else "Warning: Elevated noise levels detected.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🛡️", 
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Consensus logic protects your identity. Nudges are only sent when multiple unique sources agree.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
