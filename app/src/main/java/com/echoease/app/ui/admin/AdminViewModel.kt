package com.echoease.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.BuildingConfig
import com.echoease.app.data.model.Room
import com.echoease.app.data.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminState {
    object Loading : AdminState()
    data class Success(
        val config: BuildingConfig,
        val rooms: List<Room> = emptyList(),
        val incidentCounts: Map<String, Int> = emptyMap()
    ) : AdminState()
    data class Error(val message: String) : AdminState()
}

class AdminViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow<AdminState>(AdminState.Loading)
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                _state.value = AdminState.Loading
                val profile = repository.getUserProfile(currentUser.uid)
                if (profile == null || profile.role != "admin") {
                    _state.value = AdminState.Error("Access Denied: Admin role required")
                    return@launch
                }
                
                val config = repository.getBuildingConfig(profile.buildingId)
                val rooms = repository.getRoomsByBuilding(profile.buildingId)
                
                // Get incident counts for all rooms in this building for the heatmap
                val counts = mutableMapOf<String, Int>()
                rooms.forEach { room ->
                    val incidents = repository.getConfirmedIncidents(room.id)
                    counts[room.id] = incidents.size
                }

                _state.value = AdminState.Success(config, rooms, counts)
            } catch (e: Exception) {
                _state.value = AdminState.Error(e.message ?: "Failed to load config")
            }
        }
    }

    fun updateConfig(config: BuildingConfig) {
        viewModelScope.launch {
            try {
                repository.updateBuildingConfig(config)
                _state.value = AdminState.Success(config)
            } catch (e: Exception) {
                _state.value = AdminState.Error(e.message ?: "Failed to update config")
            }
        }
    }
}
