package com.echoease.app.data.model

import kotlinx.serialization.Serializable

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
    val name: String = "",
    val floor: Int = 0
)

@Serializable
data class UserProfile(
    val uid: String = "",
    val roomId: String = "",
    val buildingId: String = "default_building",
    val email: String? = null,
    val role: String = "resident" // "resident" or "admin"
)

@Serializable
data class BuildingConfig(
    val buildingId: String = "default_building",
    val consensusThreshold: Int = 2,
    val escalationTiers: List<Int> = listOf(2, 3, 4), // Strikes for Warning, Critical, Warden
    val wardenContact: String = ""
)
