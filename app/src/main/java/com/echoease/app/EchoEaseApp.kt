package com.echoease.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.echoease.app.util.AppConstants
import com.google.firebase.auth.FirebaseAuth

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

    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { uid ->
            val repository = RoomRepository()
            userRole = repository.getUserProfile(uid)?.role ?: "resident"
        }
    }

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
                    HomeScreen(onNavigateToDashboard = {
                        backStack.clear()
                        backStack.add(Screen.Dashboard)
                    })
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
