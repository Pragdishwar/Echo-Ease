package com.echoease.app

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.echoease.app.data.repository.RoomRepository
import com.echoease.app.ui.admin.AdminScreen
import com.echoease.app.ui.dashboard.DashboardScreen
import com.echoease.app.ui.home.HomeScreen
import com.echoease.app.ui.navigation.Screen
import com.echoease.app.ui.onboarding.AuthScreen
import com.echoease.app.ui.onboarding.BuildingSelectionScreen
import com.echoease.app.ui.onboarding.RoomSelectionScreen
import com.echoease.app.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@Composable
fun EchoEaseApp() {
    val isFirebaseAvailable = remember {
        try {
            FirebaseAuth.getInstance()
            true
        } catch (e: Exception) {
            false
        }
    }

    MyApplicationTheme {
        if (!isFirebaseAvailable) {
            FirebaseSetupRequiredScreen()
        } else {
            MainContent()
        }
    }
}

@Composable
fun MainContent() {
    val auth = FirebaseAuth.getInstance()
    val initialScreen: Screen = if (auth.currentUser == null) {
        Screen.Auth
    } else {
        Screen.Home
    }
    
    val backStack = remember { mutableStateListOf<Screen>(initialScreen) }
    val currentScreen = backStack.lastOrNull()
    var userRole by remember { mutableStateOf("resident") }
    var userRoomId by remember { mutableStateOf<String?>(null) }
    var userBuildingId by remember { mutableStateOf<String?>(null) }
    var activeNudge by remember { mutableStateOf<String?>(null) }

    // AUTH PERSISTENCE LISTENER
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && currentScreen is Screen.Auth) {
                backStack.clear()
                backStack.add(Screen.Home)
            } else if (user == null && currentScreen !is Screen.Auth) {
                backStack.clear()
                backStack.add(Screen.Auth)
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            val repository = RoomRepository()
            val profile = repository.getUserProfile(uid)
            userRole = profile?.role ?: "resident"
            userRoomId = profile?.roomId
            
            // AUTO-REDIRECT IF ROOM IS NOT SET
            if (profile?.roomId.isNullOrEmpty() && currentScreen !is Screen.BuildingSelection && currentScreen !is Screen.RoomSelection) {
                backStack.clear()
                backStack.add(Screen.BuildingSelection)
            }
        }
    }

    // REAL-TIME NUDGE LISTENER
    LaunchedEffect(userRoomId) {
        if (userRoomId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("confirmedIncidents")
                .whereEqualTo("roomId", userRoomId)
                .whereGreaterThan("timestamp", com.google.firebase.Timestamp(Date(System.currentTimeMillis() - 600000))) // Last 10 mins
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        activeNudge = "Community Notice: Multiple neighbors have flagged noise in your sector. Please keep it down. 🤫"
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentScreen is Screen.Auth || currentScreen is Screen.BuildingSelection || currentScreen is Screen.RoomSelection) {
            AppNavHost(backStack)
        } else {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        selected = currentScreen is Screen.Home,
                        onClick = { 
                            if (currentScreen !is Screen.Home) {
                                backStack.clear()
                                backStack.add(Screen.Home)
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    item(
                        selected = currentScreen is Screen.Dashboard,
                        onClick = { 
                            if (currentScreen !is Screen.Dashboard) {
                                backStack.clear()
                                backStack.add(Screen.Dashboard)
                            }
                        },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Dashboard") },
                        label = { Text("Status") }
                    )
                    if (userRole == "admin") {
                        item(
                            selected = currentScreen is Screen.Admin,
                            onClick = {
                                if (currentScreen !is Screen.Admin) {
                                    backStack.clear()
                                    backStack.add(Screen.Admin)
                                }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Admin") },
                            label = { Text("Admin") }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) {
                AppNavHost(backStack)
            }
        }

        // NUDGE BANNER
        AnimatedVisibility(
            visible = activeNudge != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = activeNudge ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { activeNudge = null }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(backStack: SnapshotStateList<Screen>) {
    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.size > 1) backStack.removeAt(backStack.size - 1) 
        },
        entryProvider = { key ->
            when (key) {
                is Screen.Auth -> NavEntry(key) {
                    AuthScreen(onAuthenticated = { 
                        backStack.add(Screen.BuildingSelection) 
                    })
                }
                is Screen.BuildingSelection -> NavEntry(key) {
                    BuildingSelectionScreen(onBuildingSelected = {
                        backStack.add(Screen.RoomSelection)
                    })
                }
                is Screen.RoomSelection -> NavEntry(key) {
                    RoomSelectionScreen(onRoomSelected = { 
                        backStack.clear()
                        backStack.add(Screen.Home) 
                    })
                }
                is Screen.Home -> NavEntry(key) {
                    HomeScreen(
                        onNavigateToDashboard = {
                            backStack.clear()
                            backStack.add(Screen.Dashboard)
                        },
                        onLogout = {
                            backStack.clear()
                            backStack.add(Screen.Auth)
                        },
                        onNavigateToOnboarding = {
                            backStack.add(Screen.BuildingSelection)
                        }
                    )
                }
                is Screen.Dashboard -> NavEntry(key) {
                    DashboardScreen()
                }
                is Screen.Admin -> NavEntry(key) {
                    AdminScreen()
                }
                else -> NavEntry(key) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unknown Screen")
                    }
                }
            }
        }
    )
}

@Composable
fun FirebaseSetupRequiredScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Firebase Configuration Missing",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "To make this app work, you MUST add your 'google-services.json' file from the Firebase Console to the 'app/' folder.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Once the file is added, the app will automatically connect to your live database and authentication service.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
