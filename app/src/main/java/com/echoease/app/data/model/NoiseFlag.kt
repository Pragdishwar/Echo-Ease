package com.echoease.app.data.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class NoiseFlag(
    val flaggerRoomId: String = "",
    val timestamp: Long = 0L,
    val timeWindow: Long = 0L
)

data class RoomAdjacency(
    val roomId: String = "",
    val neighborRoomIds: List<String> = emptyList()
)
