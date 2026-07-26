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
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import com.echoease.app.data.SupabaseClient
import com.echoease.app.data.repository.RoomRepository
import com.echoease.app.ui.admin.AdminScreen
import com.echoease.app.ui.dashboard.DashboardScreen
import com.echoease.app.ui.home.HomeScreen
import com.echoease.app.ui.navigation.Screen
import com.echoease.app.ui.onboarding.AuthScreen
import com.echoease.app.ui.onboarding.BuildingSelectionScreen
import com.echoease.app.ui.onboarding.RoomSelectionScreen
import com.echoease.app.ui.theme.MyApplicationTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EchoEaseApp() {
    MyApplicationTheme {
        MainContent()
    }
}

@Composable
fun MainContent() {
    val auth = SupabaseClient.client.auth
    val sessionStatus by auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
    
    val initialScreen: Screen = if (auth.currentUserOrNull() == null) {
        Screen.Auth
    } else {
        Screen.Home
    }
    
    val backStack = remember { mutableStateListOf(initialScreen) }
    val currentScreen = backStack.lastOrNull()
    
    var userRole by remember { mutableStateOf("resident") }
    var userRoomId by remember { mutableStateOf<String?>(null) }
    var activeNudge by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkMonitor = remember { com.echoease.app.util.NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState(initial = true)

    // AUTH PERSISTENCE LISTENER
    LaunchedEffect(auth) {
        auth.sessionStatus.collectLatest { status ->
            val current = backStack.lastOrNull()
            if (status is SessionStatus.Authenticated && current is Screen.Auth) {
                backStack.clear()
                backStack.add(Screen.Home)
            } else if (status is SessionStatus.NotAuthenticated && current !is Screen.Auth) {
                backStack.clear()
                backStack.add(Screen.Auth)
            }
        }
    }

    LaunchedEffect(sessionStatus, currentScreen) {
        if (sessionStatus is SessionStatus.Authenticated) {
            val user = auth.currentUserOrNull()
            if (user != null) {
                val repository = RoomRepository()
                val profile = repository.getUserProfile(user.id)
                userRole = profile?.role ?: "resident"
                userRoomId = profile?.roomId
                
                // AUTO-REDIRECT IF ROOM IS NOT SET
                if (profile == null || (profile.roomId.isNullOrEmpty() && currentScreen !is Screen.BuildingSelection && currentScreen !is Screen.RoomSelection)) {
                    if (currentScreen !is Screen.BuildingSelection && currentScreen !is Screen.RoomSelection) {
                        backStack.clear()
                        backStack.add(Screen.BuildingSelection)
                    }
                }
            }
        }
    }

    // REAL-TIME NUDGE LISTENER
    LaunchedEffect(userRoomId) {
        val roomId = userRoomId
        if (roomId != null) {
            val channel = SupabaseClient.client.realtime.channel("app-nudges")
            val changes = channel.postgresChangeFlow<PostgresAction.Insert>("public") {
                table = "confirmed_incidents"
                filter("room_id", FilterOperator.EQ, roomId)
            }
            
            changes.collectLatest {
                activeNudge = "Community Notice: Multiple neighbors have flagged noise in your sector. Please keep it down. \uD83E\uDD2B"
            }
            
            try {
                SupabaseClient.client.realtime.connect()
                channel.subscribe()
            } catch(e: Exception) {
                android.util.Log.e("EchoEaseApp", "Realtime error", e)
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
                    item(
                        selected = currentScreen is Screen.Profile,
                        onClick = { 
                            if (currentScreen !is Screen.Profile) {
                                backStack.clear()
                                backStack.add(Screen.Profile)
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
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
        
        // INTERNET CONNECTION DIALOG
        if (!isConnected) {
            AlertDialog(
                onDismissRequest = { /* Force them to connect, don't dismiss */ },
                title = { Text("No Internet Connection") },
                text = { Text("Echo-Ease requires an internet connection to sync noise flags and fetch consensus data. Please connect to Wi-Fi or cellular data.") },
                confirmButton = {},
                icon = { Icon(Icons.Default.WifiOff, null) }
            )
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
                    BuildingSelectionScreen(onBuildingSelected = { buildingId ->
                        backStack.add(Screen.RoomSelection(buildingId))
                    })
                }
                is Screen.RoomSelection -> NavEntry(key) {
                    RoomSelectionScreen(
                        buildingId = key.buildingId,
                        onRoomSelected = { 
                            backStack.clear()
                            backStack.add(Screen.Home) 
                        }
                    )
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
                is Screen.Profile -> NavEntry(key) {
                    com.echoease.app.ui.profile.ProfileScreen(
                        onNavigateToOnboarding = {
                            backStack.add(Screen.BuildingSelection)
                        },
                        onLogout = {
                            backStack.clear()
                            backStack.add(Screen.Auth)
                        }
                    )
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
