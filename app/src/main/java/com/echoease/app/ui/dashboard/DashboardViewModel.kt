package com.echoease.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.model.ConfirmedIncident
import com.echoease.app.data.repository.RoomRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import com.echoease.app.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
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
    private val auth = SupabaseClient.client.auth
    private val realtime = SupabaseClient.client.realtime
    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()
    
    private var isRealtimeConnected = false

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val currentUser = auth.currentUserOrNull() ?: return
        viewModelScope.launch {
            try {
                _state.value = DashboardState.Loading
                val profile = repository.getUserProfile(currentUser.id)
                if (profile == null || profile.roomId.isNullOrEmpty()) {
                    _state.value = DashboardState.Error("Room not set up")
                    return@launch
                }
                
                val roomId = profile.roomId ?: ""
                
                updateDashboardData(roomId)
                
                if (!isRealtimeConnected) {
                    val channel = realtime.channel("dashboard-updates")
                    
                    val flagsFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                        table = "flags"
                        filter("flagger_room_id", FilterOperator.EQ, roomId)
                    }
                    flagsFlow.onEach { updateDashboardData(roomId) }.launchIn(viewModelScope)
                    
                    val incidentsFlow = channel.postgresChangeFlow<PostgresAction>("public") {
                        table = "confirmed_incidents"
                        filter("room_id", FilterOperator.EQ, roomId)
                    }
                    incidentsFlow.onEach { updateDashboardData(roomId) }.launchIn(viewModelScope)
                    
                    realtime.connect()
                    channel.subscribe()
                    isRealtimeConnected = true
                }
                    
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Failed to load dashboard")
            }
        }
    }

    private suspend fun updateDashboardData(roomId: String) {
        try {
            val myIncidents = repository.getConfirmedIncidents(roomId)
            val flaggedByMe = repository.getIncidentsByFlagger(roomId)
            
            val strikeLevel = myIncidents.size
            
            _state.value = DashboardState.Success(myIncidents, flaggedByMe, strikeLevel)
        } catch (e: Exception) {
            _state.value = DashboardState.Error(e.message ?: "Failed to load dashboard")
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            try {
                realtime.disconnect()
            } catch(e: Exception) {
                android.util.Log.e("DashboardViewModel", "Disconnect error", e)
            }
        }
    }
}
