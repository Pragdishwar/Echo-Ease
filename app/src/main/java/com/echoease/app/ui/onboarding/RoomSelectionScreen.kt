package com.echoease.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSelectionScreen(
    onRoomSelected: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val rooms by viewModel.rooms.collectAsState()
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(state) {
        if (state is OnboardingState.RoomSelected) {
            onRoomSelected()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Select Your Space") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Locating your floor...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(rooms) { room ->
                        ListItem(
                            headlineContent = { Text(room.name, style = MaterialTheme.typography.titleLarge) },
                            supportingContent = { Text("Floor ${room.floor}", style = MaterialTheme.typography.bodyMedium) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.clickable {
                                viewModel.selectRoom(room.id)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }

            if (state is OnboardingState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            if (state is OnboardingState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                ) {
                    Text(
                        text = (state as OnboardingState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoomSelectionPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                LargeTopAppBar(title = { Text("Select Your Space") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                ListItem(headlineContent = { Text("Room 101") }, supportingContent = { Text("Floor 1") })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(headlineContent = { Text("Room 102") }, supportingContent = { Text("Floor 1") })
            }
        }
    }
}
