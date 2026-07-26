package com.echoease.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.util.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingSelectionScreen(
    onBuildingSelected: (String) -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val buildings by viewModel.buildings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Select Your Building") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (buildings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Looking for buildings...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "If this takes too long, your database might be empty.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(onClick = { showHelp = true }) {
                            Text("I don't see my building")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(buildings) { building ->
                        ListItem(
                            headlineContent = { Text(building.name) },
                            supportingContent = { Text("${building.address}, ${building.city}") },
                            leadingContent = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.clickable {
                                viewModel.selectBuilding(building.id)
                                onBuildingSelected(building.id)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
            
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    icon = { Icon(Icons.Default.Warning, null) },
                    title = { Text("Empty Database") },
                    text = {
                        Text("It looks like there are no buildings registered in your Firestore yet. \n\nWould you like to enable 'Demo Mode' with sample data so you can explore the app?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            AppConstants.USE_MOCK_DATA = true
                            showHelp = false
                            onBuildingSelected("default_building") // Refresh or navigate
                        }) {
                            Text("Enable Demo Mode")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showHelp = false }) {
                            Text("Wait")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BuildingSelectionPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                LargeTopAppBar(title = { Text("Select Your Building") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                ListItem(
                    headlineContent = { Text("Echo Hostel") },
                    supportingContent = { Text("123 Main St, Tech City") },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Ease Apartments") },
                    supportingContent = { Text("456 Oak Rd, Nature City") },
                    leadingContent = { Icon(Icons.Default.LocationOn, null) }
                )
                HorizontalDivider()
            }
        }
    }
}
