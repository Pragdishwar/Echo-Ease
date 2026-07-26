package com.echoease.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.Building
import com.echoease.app.data.model.Room
import com.echoease.app.data.model.UserProfile
import com.echoease.app.data.repository.RoomRepository
import com.echoease.app.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    data class Authenticated(val uid: String) : OnboardingState()
    object RoomSelected : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

class OnboardingViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth = SupabaseClient.client.auth

    private val _state = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings.asStateFlow()

    private var selectedBuildingId: String = ""

    init {
        loadBuildings()
    }

    private fun loadBuildings() {
        viewModelScope.launch {
            _buildings.value = repository.getAllBuildings()
        }
    }

    fun reset() {
        _state.value = OnboardingState.Idle
        selectedBuildingId = ""
    }

    fun selectBuilding(buildingId: String) {
        selectedBuildingId = buildingId
        loadRoomsForBuilding(buildingId)
    }

    fun loadRoomsForBuilding(buildingId: String) {
        selectedBuildingId = buildingId
        viewModelScope.launch {
            _rooms.value = repository.getRoomsByBuilding(buildingId)
        }
    }

    fun selectRoom(roomId: String) {
        val currentUser = auth.currentUserOrNull()
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _state.value = OnboardingState.Loading
                    val existingProfile = repository.getUserProfile(currentUser.id)
                    val googleName = currentUser.userMetadata?.get("full_name")?.jsonPrimitive?.content ?: currentUser.userMetadata?.get("name")?.jsonPrimitive?.content
                    val profile = UserProfile(
                        uid = currentUser.id,
                        roomId = roomId,
                        buildingId = selectedBuildingId,
                        name = existingProfile?.name?.takeIf { it.isNotBlank() } ?: googleName,
                        email = currentUser.email,
                        role = existingProfile?.role ?: "resident"
                    )
                    repository.saveUserProfile(profile)
                    _state.value = OnboardingState.RoomSelected
                } catch (e: Exception) {
                    _state.value = OnboardingState.Error(e.message ?: "Failed to save profile")
                }
            }
        }
    }
}
