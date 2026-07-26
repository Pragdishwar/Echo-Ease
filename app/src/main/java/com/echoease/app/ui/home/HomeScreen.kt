package com.echoease.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.ui.components.LiveWaveform
import com.echoease.app.util.AudioAnalyzer
import com.echoease.app.util.AudioRecorder
import com.echoease.app.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDashboard: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showAmbientCheck by remember { mutableStateOf(false) }
    
    val audioAnalyzer = remember { AudioAnalyzer() }
    val audioRecorder = remember { AudioRecorder(context) }
    val currentDb by audioAnalyzer.decibels.collectAsState()
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var recordProgress by remember { mutableFloatStateOf(0f) }
    var hasSample by remember { mutableStateOf(false) }

    // REFRESH PROFILE WHEN SCREEN LOADS
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            showAmbientCheck = true
            audioAnalyzer.start(scope)
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    DisposableEffect(showAmbientCheck) {
        onDispose {
            audioAnalyzer.stop()
            audioRecorder.cleanup()
        }
    }

    LaunchedEffect(state) {
        when (state) {
            is HomeState.Success -> {
                snackbarHostState.showSnackbar("Your report has been submitted anonymously.")
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification("Report Submitted", "Your noise flag has been securely recorded.")
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
            // Keep the TopBar minimal or remove if we want it strictly like the image
            // Let's keep a very simple one for the Room display
            if (userProfile != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Room ${userProfile?.roomId ?: "Not Set"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        HomeContent(
            padding = padding,
            isLoading = state is HomeState.Loading,
            onFlagNoise = { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                viewModel.flagNoise(audioRecorder.getFile()) 
            },
            onAmbientCheck = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) -> {
                        showAmbientCheck = true
                        audioAnalyzer.start(scope)
                    }
                    else -> {
                        permissionLauncher.launch(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                arrayOf(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    }
                }
            },
            onSelectRoom = {}, // Moved to Profile Screen
            onNavigateToHistory = onNavigateToDashboard,
            onProfileClick = {}, // Moved to bottom nav
            onUpdateName = {}, // Moved to Profile Screen
            userProfile = userProfile,
            onLogout = {} // Moved to Profile Screen
        )

        if (showAmbientCheck) {
            AlertDialog(
                onDismissRequest = { 
                    showAmbientCheck = false
                    audioAnalyzer.stop()
                    audioRecorder.cleanup()
                    isRecording = false
                    hasSample = false
                },
                confirmButton = {
                    Button(onClick = { 
                        showAmbientCheck = false
                        audioAnalyzer.stop()
                        audioRecorder.cleanup()
                        isRecording = false
                        hasSample = false
                    }) { Text("DONE") }
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
                        
                        LiveWaveform(db = currentDb)

                        Text(
                            if (currentDb < 60) "Ambient level is currently SAFE." else "Warning: Elevated noise levels detected.", 
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (isRecording) {
                            LinearProgressIndicator(
                                progress = { recordProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text("Recording sample...", style = MaterialTheme.typography.labelSmall)
                        } else if (hasSample) {
                            Button(
                                onClick = { audioRecorder.playSample() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAYBACK SAMPLE")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { 
                                    showAmbientCheck = false
                                    audioAnalyzer.stop()
                                    viewModel.flagNoise(audioRecorder.getFile())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Send, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SUBMIT FLAG & AUDIO")
                            }

                            TextButton(onClick = { 
                                hasSample = false
                            }) {
                                Text("RE-RECORD")
                            }
                        } else {
                            Button(
                                onClick = { 
                                    isRecording = true
                                    hasSample = false
                                    recordProgress = 0f
                                    
                                    // PAUSE ANALYZER TO RELEASE MIC HARDWARE
                                    audioAnalyzer.stop()
                                    
                                    audioRecorder.startRecording()
                                    scope.launch {
                                        for (i in 1..50) {
                                            delay(100)
                                            recordProgress = i / 50f
                                            
                                            // Keep visualizer alive during recording using MediaRecorder's amplitude!
                                            val maxAmp = audioRecorder.getMaxAmplitude()
                                            if (maxAmp > 0) {
                                                val dbfs = 20 * kotlin.math.log10(maxAmp.toDouble() / 32768.0)
                                                val db = (dbfs + 110.0 - 3.0).coerceAtLeast(0.0)
                                                audioAnalyzer.setManualDb(db)
                                            } else {
                                                audioAnalyzer.setManualDb(0.0) // real silence
                                            }
                                        }
                                        audioRecorder.stopRecording()
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        isRecording = false
                                        hasSample = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.FiberManualRecord, null)
                                Text("RECORD 5s SAMPLE")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun HomeContent(
    padding: PaddingValues,
    isLoading: Boolean,
    onFlagNoise: () -> Unit,
    onAmbientCheck: () -> Unit,
    onSelectRoom: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onProfileClick: () -> Unit,
    onUpdateName: (String) -> Unit,
    userProfile: com.echoease.app.data.model.UserProfile?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // HEADER FROM IMAGE
        Text(
            text = "EchoEase",
            style = MaterialTheme.typography.headlineSmall, // Shrank from displaySmall
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp)) // Shrank from 16dp

        // SHIELD ICON
        Box(
            modifier = Modifier.size(60.dp), // Shrank from 80dp
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(32.dp), // Shrank from 40dp
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp)) // Shrank from 16dp

        Text(
            text = "Peaceful Living,\nRespectfully.",
            style = MaterialTheme.typography.titleMedium, // Shrank from titleLarge
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp)) // Shrank from 40dp

        // MENU BUTTONS
        MenuButton(
            text = "Silent Flag",
            icon = Icons.Default.NotificationsPaused,
            isPrimary = true,
            isLoading = isLoading,
            onClick = onFlagNoise
        )

        Spacer(modifier = Modifier.height(8.dp)) // Shrank from 12dp

        MenuButton(
            text = "Ambient Sound Check",
            icon = Icons.Default.GraphicEq,
            onClick = onAmbientCheck
        )

        Spacer(modifier = Modifier.height(8.dp))

        MenuButton(
            text = "History",
            icon = Icons.Default.History,
            onClick = onNavigateToHistory
        )
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // Shrank from 64dp
        shape = MaterialTheme.shapes.large,
        colors = if (isPrimary) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        },
        border = if (!isPrimary) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isPrimary) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(20.dp))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    MaterialTheme {
        HomeContent(
            padding = PaddingValues(0.dp),
            isLoading = false,
            onFlagNoise = {},
            onAmbientCheck = {},
            onSelectRoom = {},
            onNavigateToHistory = {},
            onProfileClick = {},
            onUpdateName = {},
            userProfile = null,
            onLogout = {}
        )
    }
}
