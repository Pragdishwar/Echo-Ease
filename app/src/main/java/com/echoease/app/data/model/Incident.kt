package com.echoease.app.data.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmedIncident(
    val id: String = "",
    val roomId: String = "",
    val flaggerRoomId: String? = null, // New field to track who flagged
    val timestamp: Long = 0L,
    val severity: Int = 1,
    val audioProofUrl: String? = null,
    val isWardenEscalated: Boolean = false, // New field for escalation
    val status: String = "Confirmed"
)
