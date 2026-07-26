package com.echoease.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.BuildingConfig
import com.echoease.app.data.model.Room
import com.echoease.app.data.repository.RoomRepository
import com.echoease.app.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminState {
    object Loading : AdminState()
    data class Success(
        val config: BuildingConfig,
        val rooms: List<Room> = emptyList(),
        val incidentCounts: Map<String, Int> = emptyMap(),
        val escalatedIncidents: List<com.echoease.app.data.model.ConfirmedIncident> = emptyList(),
        val users: List<com.echoease.app.data.model.UserProfile> = emptyList()
    ) : AdminState()
    data class Error(val message: String) : AdminState()
}

class AdminViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth = SupabaseClient.client.auth

    private val _state = MutableStateFlow<AdminState>(AdminState.Loading)
    val state: StateFlow<AdminState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        val currentUser = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                _state.value = AdminState.Loading
                val profile = repository.getUserProfile(currentUser.id)
                if (profile == null || profile.role != "admin") {
                    _state.value = AdminState.Error("Access Denied: Admin role required")
                    return@launch
                }
                
                val buildingId = profile.buildingId ?: "default_building"
                
                val config = repository.getBuildingConfig(buildingId)
                val rooms = repository.getRoomsByBuilding(buildingId)
                
                // Get incident counts for all rooms in this building for the heatmap
                val counts = mutableMapOf<String, Int>()
                rooms.forEach { room ->
                    val incidents = repository.getConfirmedIncidents(room.id)
                    counts[room.id] = incidents.size
                }
                
                val escalatedIncidents = repository.getEscalatedIncidents(buildingId)
                val users = repository.getAllUsersInBuilding(buildingId)

                _state.value = AdminState.Success(config, rooms, counts, escalatedIncidents, users)
            } catch (e: Exception) {
                _state.value = AdminState.Error(e.message ?: "Failed to load config")
            }
        }
    }

    fun updateConfig(config: BuildingConfig) {
        val currentState = _state.value
        if (currentState !is AdminState.Success) return
        
        viewModelScope.launch {
            try {
                repository.updateBuildingConfig(config)
                _state.value = currentState.copy(config = config)
            } catch (e: Exception) {
                _state.value = AdminState.Error(e.message ?: "Failed to update config")
            }
        }
    }
    fun updateUserRoom(userId: String, newRoomId: String) {
        viewModelScope.launch {
            try {
                repository.updateUserRoom(userId, newRoomId)
                loadConfig() // refresh data
            } catch (e: Exception) {
                // Should show error in UI ideally
                android.util.Log.e("AdminViewModel", "Error updating room", e)
            }
        }
    }

    fun addRoom(id: String, name: String, floor: Int) {
        val currentState = _state.value
        if (currentState !is AdminState.Success) return
        viewModelScope.launch {
            try {
                val newRoom = Room(id = id, name = name, floor = floor)
                repository.addRoom(newRoom, currentState.config.buildingId)
                loadConfig() // refresh data
            } catch (e: Exception) {
                _state.value = AdminState.Error("Error adding room: ${e.message}")
            }
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoom(roomId)
                loadConfig() // refresh data
            } catch (e: Exception) {
                _state.value = AdminState.Error("Error deleting room: ${e.message}")
            }
        }
    }

    fun updateRoom(roomId: String, name: String, floor: Int) {
        viewModelScope.launch {
            try {
                repository.updateRoom(roomId, name, floor)
                loadConfig()
            } catch (e: Exception) {
                _state.value = AdminState.Error("Error updating room: ${e.message}")
            }
        }
    }

    fun resolveEscalation(incidentId: String) {
        viewModelScope.launch {
            try {
                repository.resolveEscalation(incidentId)
                loadConfig()
            } catch (e: Exception) {
                _state.value = AdminState.Error("Failed to resolve escalation: ${e.message}")
            }
        }
    }
}
