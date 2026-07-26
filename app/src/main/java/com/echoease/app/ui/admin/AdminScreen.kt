package com.echoease.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echoease.app.data.model.BuildingConfig
import com.echoease.app.data.model.Room
import com.echoease.app.ui.components.FloorHeatmap

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
        },
        contentWindowInsets = WindowInsets.systemBars
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
                    val successState = state as AdminState.Success
                    AdminContent(
                        config = successState.config,
                        rooms = successState.rooms,
                        incidentCounts = successState.incidentCounts,
                        escalatedIncidents = successState.escalatedIncidents,
                        users = successState.users,
                        onSave = { viewModel.updateConfig(it) },
                        onUpdateUserRoom = { userId, newRoomId -> viewModel.updateUserRoom(userId, newRoomId) },
                        onAddRoom = { id, name, floor -> viewModel.addRoom(id, name, floor) },
                        onDeleteRoom = { roomId -> viewModel.deleteRoom(roomId) },
                        onEditRoom = { id, name, floor -> viewModel.updateRoom(id, name, floor) },
                        onResolveEscalation = { incidentId -> viewModel.resolveEscalation(incidentId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContent(
    config: BuildingConfig,
    rooms: List<Room> = emptyList(),
    incidentCounts: Map<String, Int> = emptyMap(),
    escalatedIncidents: List<com.echoease.app.data.model.ConfirmedIncident> = emptyList(),
    users: List<com.echoease.app.data.model.UserProfile> = emptyList(),
    onSave: (BuildingConfig) -> Unit,
    onUpdateUserRoom: (String, String) -> Unit,
    onAddRoom: (String, String, Int) -> Unit,
    onDeleteRoom: (String) -> Unit,
    onEditRoom: (String, String, Int) -> Unit,
    onResolveEscalation: (String) -> Unit
) {
    var threshold by remember { mutableIntStateOf(config.consensusThreshold) }
    var wardenThreshold by remember { mutableIntStateOf(config.escalationTiers.lastOrNull() ?: 4) }
    var wardenContact by remember { mutableStateOf(config.wardenContact) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // --- BUILDING STRUCTURE (ROOMS) ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Building Structure", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { showAddRoomDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Room")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Room")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ID", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Name", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Floor", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Action", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                if (rooms.isEmpty()) {
                    Text("No rooms configured.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
                } else {
                    rooms.forEach { room ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(room.id, modifier = Modifier.weight(0.5f))
                            Text(room.name?.takeIf { it.isNotBlank() } ?: "-", modifier = Modifier.weight(1.5f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(room.floor?.toString() ?: "-", modifier = Modifier.weight(0.5f))
                            Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.CenterEnd) {
                                Row {
                                    IconButton(onClick = { editingRoom = room }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Room", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onDeleteRoom(room.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Room", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        if (room != rooms.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        if (showAddRoomDialog) {
            var newRoomId by remember { mutableStateOf("") }
            var newRoomName by remember { mutableStateOf("") }
            var newRoomFloor by remember { mutableStateOf("") }
            
            val isIdBlank = newRoomId.isBlank()
            val hasSpace = newRoomId.contains(" ")
            val isDuplicate = rooms.any { it.id == newRoomId }
            val idHasError = newRoomId.isNotEmpty() && (hasSpace || isDuplicate)
            val isFloorBlank = newRoomFloor.isBlank()
            val isValid = !isIdBlank && !idHasError && !isFloorBlank

            AlertDialog(
                onDismissRequest = { showAddRoomDialog = false },
                title = { Text("Add New Room") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newRoomId,
                            onValueChange = { newRoomId = it },
                            label = { Text("Room ID (e.g. 501)") },
                            singleLine = true,
                            isError = idHasError,
                            supportingText = {
                                if (hasSpace) Text("ID cannot contain spaces")
                                else if (isDuplicate) Text("Room ID already exists")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newRoomName,
                            onValueChange = { newRoomName = it },
                            label = { Text("Custom Name (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandedFloor by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedFloor,
                            onExpandedChange = { expandedFloor = it }
                        ) {
                            OutlinedTextField(
                                value = newRoomFloor,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Floor Number") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFloor)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedFloor,
                                onDismissRequest = { expandedFloor = false }
                            ) {
                                for (i in 1..6) {
                                    DropdownMenuItem(
                                        text = { Text("Floor $i") },
                                        onClick = {
                                            newRoomFloor = i.toString()
                                            expandedFloor = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val floorInt = newRoomFloor.toIntOrNull() ?: 0
                            if (newRoomId.isNotBlank()) {
                                onAddRoom(newRoomId, newRoomName, floorInt)
                                showAddRoomDialog = false
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddRoomDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (editingRoom != null) {
            var editRoomName by remember(editingRoom) { mutableStateOf(editingRoom?.name ?: "") }
            var editRoomFloor by remember(editingRoom) { mutableStateOf(editingRoom?.floor?.toString() ?: "") }
            
            val isFloorBlank = editRoomFloor.isBlank()

            AlertDialog(
                onDismissRequest = { editingRoom = null },
                title = { Text("Edit Room ${editingRoom?.id}") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editRoomName,
                            onValueChange = { editRoomName = it },
                            label = { Text("Custom Name (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandedEditFloor by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedEditFloor,
                            onExpandedChange = { expandedEditFloor = it }
                        ) {
                            OutlinedTextField(
                                value = editRoomFloor,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Floor Number") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEditFloor)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedEditFloor,
                                onDismissRequest = { expandedEditFloor = false }
                            ) {
                                for (i in 1..6) {
                                    DropdownMenuItem(
                                        text = { Text("Floor $i") },
                                        onClick = {
                                            editRoomFloor = i.toString()
                                            expandedEditFloor = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val floorInt = editRoomFloor.toIntOrNull() ?: 0
                            editingRoom?.id?.let { roomId ->
                                onEditRoom(roomId, editRoomName, floorInt)
                            }
                            editingRoom = null
                        },
                        enabled = !isFloorBlank
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingRoom = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BUILDING CONFIGURATION ---
        Text("Building Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (escalatedIncidents.isEmpty()) {
            Text(
                text = "No escalated incidents requiring attention.", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            escalatedIncidents.forEach { incident ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Action Required for Room ${incident.roomId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Repeated offender: Severity ${incident.severity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Last Incident: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(incident.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Button(
                            onClick = { onResolveEscalation(incident.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text("Resolve")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 32.dp))

        Text(
            text = "Mediation Tuning",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // HEATMAP SECTION
        if (rooms.isNotEmpty()) {
            FloorHeatmap(
                rooms = rooms,
                incidentCounts = incidentCounts,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 32.dp))
        }

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
                
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                OutlinedTextField(
                    value = wardenContact,
                    onValueChange = { wardenContact = it },
                    label = { Text("Warden Contact (Email/Phone)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
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

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 32.dp))

        Text(
            text = "Resident Management",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (users.isEmpty()) {
            Text("No residents found in this building.", color = MaterialTheme.colorScheme.outline)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Table Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Resident", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Room", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Action", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    users.forEach { user ->
                        var showRoomDropdown by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                val displayName = user.name?.takeIf { it.isNotBlank() } ?: user.email ?: "Resident"
                                Text(displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                if (user.role == "admin") {
                                    Text("Admin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = user.roomId.takeIf { !it.isNullOrEmpty() } ?: "None", 
                                modifier = Modifier.weight(0.8f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                TextButton(
                                    onClick = { showRoomDropdown = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Edit Room")
                                }
                                DropdownMenu(
                                    expanded = showRoomDropdown,
                                    onDismissRequest = { showRoomDropdown = false }
                                ) {
                                    rooms.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text("Room ${room.id}") },
                                            onClick = {
                                                showRoomDropdown = false
                                                onUpdateUserRoom(user.uid, room.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (user != users.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminContentPreview() {
    MaterialTheme {
        AdminContent(
            config = BuildingConfig(
                consensusThreshold = 3,
                escalationTiers = listOf(2, 3, 5),
                wardenContact = "manager@echoease.com"
            ),
            onSave = {},
            onUpdateUserRoom = { _, _ -> },
            onAddRoom = { _, _, _ -> },
            onDeleteRoom = { _ -> },
            onEditRoom = { _, _, _ -> },
            onResolveEscalation = { _ -> }
        )
    }
}
