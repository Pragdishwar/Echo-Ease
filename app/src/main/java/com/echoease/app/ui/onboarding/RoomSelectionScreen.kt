package com.echoease.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.data.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSelectionScreen(
    buildingId: String,
    onRoomSelected: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val rooms by viewModel.rooms.collectAsState()
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(buildingId) {
        viewModel.loadRoomsForBuilding(buildingId)
    }

    LaunchedEffect(state) {
        if (state is OnboardingState.RoomSelected) {
            onRoomSelected()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Select Your Space", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Locating your floor plan...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                val floors = remember(rooms) { rooms.mapNotNull { it.floor }.distinct().sorted() }
                if (floors.isEmpty()) {
                    Text("No rooms configured.", modifier = Modifier.align(Alignment.Center))
                } else {
                    var selectedFloor by remember { mutableStateOf(floors.first()) }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = floors.indexOf(selectedFloor).coerceAtLeast(0),
                            edgePadding = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            floors.forEach { floor ->
                                Tab(
                                    selected = selectedFloor == floor,
                                    onClick = { selectedFloor = floor },
                                    text = { Text("Floor $floor", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        
                        AnimatedContent(
                            targetState = selectedFloor,
                            transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                            label = "floor_animation"
                        ) { targetFloor ->
                            val roomsOnFloor = rooms.filter { it.floor == targetFloor }.sortedBy { it.id }
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(roomsOnFloor) { room ->
                                    val displayName = room.name?.takeIf { it.isNotBlank() } ?: "Room ${room.id}"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clickable { viewModel.selectRoom(room.id) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
