package com.echoease.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echoease.app.data.local.PreferenceManager
import com.echoease.app.data.model.NoiseFlag
import com.echoease.app.data.repository.RoomRepository
import com.echoease.app.util.AppConstants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class HomeState {
    object Idle : HomeState()
    object Loading : HomeState()
    object Success : HomeState()
    data class Error(val message: String) : HomeState()
    data class RateLimited(val remainingMillis: Long) : HomeState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RoomRepository()
    private val preferenceManager = PreferenceManager(application)
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _state = MutableStateFlow<HomeState>(HomeState.Idle)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _userProfile = MutableStateFlow<com.echoease.app.data.model.UserProfile?>(null)
    val userProfile: StateFlow<com.echoease.app.data.model.UserProfile?> = _userProfile.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            _userProfile.value = repository.getUserProfile(currentUser.uid)
        }
    }

    fun flagNoise(audioFile: java.io.File? = null) {
        val currentUser = auth.currentUser ?: return
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            try {
                _state.value = HomeState.Loading
                
                val lastFlag = preferenceManager.lastFlagTimestamp.first()
                val cooldown = AppConstants.FLAG_RATE_LIMIT_MS
                
                if (now - lastFlag < cooldown) {
                    _state.value = HomeState.RateLimited(cooldown - (now - lastFlag))
                    return@launch
                }

                val profile = repository.getUserProfile(currentUser.uid)
                if (profile == null || profile.roomId.isEmpty()) {
                    _state.value = HomeState.Error("Please set up your room first")
                    return@launch
                }

                var audioUrl: String? = null
                if (audioFile != null && audioFile.exists()) {
                    audioUrl = repository.uploadAudioProof(audioFile, profile.buildingId)
                }

                val windowSize = AppConstants.CONSENSUS_WINDOW_MS
                val timeWindow = (now / windowSize) * windowSize

                val flag = NoiseFlag(
                    flaggerRoomId = profile.roomId,
                    timestamp = now,
                    timeWindow = timeWindow
                )

                repository.flagNoise(flag, profile.buildingId, audioUrl)
                preferenceManager.updateLastFlagTimestamp(now)
                _state.value = HomeState.Success
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _state.value = HomeState.Idle
    }

    fun signOut() {
        auth.signOut()
    }
}
