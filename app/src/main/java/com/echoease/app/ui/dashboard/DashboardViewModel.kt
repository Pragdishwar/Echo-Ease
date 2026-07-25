package com.echoease.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.ConfirmedIncident
import com.echoease.app.data.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val myIncidents: List<ConfirmedIncident>,
        val flaggedByMe: List<ConfirmedIncident>,
        val strikeLevel: Int
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                _state.value = DashboardState.Loading
                val profile = repository.getUserProfile(currentUser.uid)
                if (profile == null || profile.roomId.isEmpty()) {
                    _state.value = DashboardState.Error("Room not set up")
                    return@launch
                }
                
                val roomId = profile.roomId
                val myIncidents = repository.getConfirmedIncidents(roomId)
                val flaggedByMe = repository.getIncidentsByFlagger(roomId)
                
                val strikeLevel = myIncidents.size
                
                _state.value = DashboardState.Success(myIncidents, flaggedByMe, strikeLevel)
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Failed to load dashboard")
            }
        }
    }
}
