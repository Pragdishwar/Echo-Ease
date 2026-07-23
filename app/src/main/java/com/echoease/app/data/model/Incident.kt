package com.echoease.app.data.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmedIncident(
    val id: String = "",
    val roomId: String = "",
    val timestamp: Long = 0L,
    val severity: Int = 1
)
