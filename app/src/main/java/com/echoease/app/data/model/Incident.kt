package com.echoease.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ConfirmedIncident(
    val id: String = "",
    @SerialName("room_id") val roomId: String = "",
    @SerialName("flagger_room_id") val flaggerRoomId: String? = null,
    val timestamp: Long = 0L,
    val severity: Int = 1,
    @SerialName("audio_proof_url") val audioProofUrl: String? = null,
    @SerialName("is_warden_escalated") val isWardenEscalated: Boolean = false,
    val status: String = "Confirmed"
)
