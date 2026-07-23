package com.echoease.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.Building
import com.echoease.app.data.model.Room
import com.echoease.app.data.model.UserProfile
import com.echoease.app.data.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    data class Authenticated(val uid: String) : OnboardingState()
    object RoomSelected : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

class OnboardingViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }

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

    fun selectBuilding(buildingId: String) {
        selectedBuildingId = buildingId
        viewModelScope.launch {
            _rooms.value = repository.getRoomsByBuilding(buildingId)
        }
    }

    fun selectRoom(roomId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _state.value = OnboardingState.Loading
                    val profile = UserProfile(
                        uid = currentUser.uid,
                        roomId = roomId,
                        buildingId = selectedBuildingId,
                        email = currentUser.email
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
