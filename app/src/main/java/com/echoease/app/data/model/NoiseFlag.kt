package com.echoease.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class NoiseFlag(
    @SerialName("flagger_room_id") val flaggerRoomId: String = "",
    val timestamp: Long = 0L,
    @SerialName("time_window") val timeWindow: Long = 0L,
    @SerialName("audio_url") val audioUrl: String? = null
)

data class RoomAdjacency(
    val roomId: String = "",
    val neighborRoomIds: List<String> = emptyList()
)
