package com.echoease.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Building(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = ""
)

@Serializable
data class Room(
    val id: String = "",
    val name: String? = "",
    val floor: Int? = 0
)

@Serializable
data class UserProfile(
    @SerialName("id") val uid: String = "",
    @SerialName("room_id") val roomId: String? = "",
    @SerialName("building_id") val buildingId: String? = "default_building",
    val name: String? = null,
    val email: String? = null,
    val role: String? = "resident", // "resident" or "admin"
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BuildingConfig(
    @SerialName("id") val buildingId: String = "default_building",
    @SerialName("consensus_threshold") val consensusThreshold: Int = 2,
    @SerialName("escalation_tiers") val escalationTiers: List<Int> = listOf(2, 3, 4), // Strikes for Warning, Critical, Warden
    @SerialName("warden_contact") val wardenContact: String = ""
)
