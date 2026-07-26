package com.echoease.app.data.repository

import com.echoease.app.data.SupabaseClient
import com.echoease.app.data.model.*
import com.echoease.app.util.AppConstants
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class RoomRepository {
    private val postgrest = SupabaseClient.client.postgrest

    suspend fun getAllBuildings(): List<Building> {
        if (AppConstants.USE_MOCK_DATA) {
            return listOf(
                Building("b1", "Echo Hostel", "123 Main St", "Tech City"),
                Building("b2", "Ease Apartments", "456 Oak Rd", "Nature City")
            )
        }
        return listOf(
            Building("default_building", "EchoEase HQ", "123 Main", "City")
        ) // We are using a single building_config for now
    }

    suspend fun getRoomsByBuilding(buildingId: String): List<Room> {
        if (AppConstants.USE_MOCK_DATA) {
            return listOf(
                Room("101", "Room 101", 1), Room("102", "Room 102", 1), Room("201", "Room 201", 2)
            )
        }
        return try {
            postgrest["rooms"].select {
                filter {
                    eq("building_id", buildingId)
                }
            }.decodeList<Room>()
        } catch (e: Exception) {
            android.util.Log.e("RoomRepository", "Error fetching rooms", e)
            emptyList()
        }
    }

    suspend fun addRoom(room: Room, buildingId: String) {
        if (AppConstants.USE_MOCK_DATA) return
        @kotlinx.serialization.Serializable
        data class InsertRoom(
            val id: String,
            @kotlinx.serialization.SerialName("building_id") val buildingId: String,
            val name: String?,
            val floor: Int?
        )
        postgrest["rooms"].insert(InsertRoom(room.id, buildingId, room.name, room.floor))
    }

    suspend fun deleteRoom(roomId: String) {
        if (AppConstants.USE_MOCK_DATA) return
        postgrest["rooms"].delete {
            filter {
                eq("id", roomId)
            }
        }
    }

    suspend fun updateRoom(roomId: String, name: String?, floor: Int?) {
        if (AppConstants.USE_MOCK_DATA) return
        postgrest["rooms"].update(
            {
                set("name", name)
                set("floor", floor)
            }
        ) {
            filter {
                eq("id", roomId)
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        postgrest["users"].upsert(profile)
    }

    suspend fun updateUserName(uid: String, name: String) {
        postgrest["users"].update(
            {
                set("name", name)
            }
        ) {
            filter { eq("id", uid) }
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        if (AppConstants.USE_MOCK_DATA) {
            return UserProfile(uid, "101", "default_building", "user@example.com", "resident")
        }
        return try {
            val result = postgrest["users"].select {
                filter {
                    eq("id", uid)
                }
            }
            if (result.data == "[]") return null
            result.decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            // Return a fallback profile so the user isn't trapped in onboarding loop
            UserProfile(uid = uid, roomId = "fetch_error", name = e.message ?: "Unknown Error", buildingId = "default_building", role = "resident")
        }
    }

    suspend fun getAllUsersInBuilding(buildingId: String): List<UserProfile> {
        if (AppConstants.USE_MOCK_DATA) {
            return emptyList()
        }
        return try {
            postgrest["users"].select().decodeList<UserProfile>()
        } catch (e: Exception) {
            android.util.Log.e("RoomRepository", "Error fetching all users in building", e)
            listOf(UserProfile(uid = "error", email = "Error: ${e.message ?: e.javaClass.simpleName}", role = "admin"))
        }
    }

    suspend fun updateUserRoom(userId: String, newRoomId: String) {
        if (AppConstants.USE_MOCK_DATA) return
        
        @kotlinx.serialization.Serializable
        data class RoomUpdate(
            @kotlinx.serialization.SerialName("room_id") val roomId: String
        )
        postgrest["users"].update(
            value = RoomUpdate(newRoomId)
        ) {
            filter {
                eq("id", userId)
            }
        }
    }

    suspend fun getBuildingConfig(buildingId: String): BuildingConfig {
        return try {
            postgrest["building_config"].select {
                filter {
                    eq("id", buildingId)
                }
            }.decodeSingleOrNull<BuildingConfig>() ?: BuildingConfig(buildingId)
        } catch (e: Exception) {
            BuildingConfig(buildingId)
        }
    }

    suspend fun updateBuildingConfig(config: BuildingConfig) {
        postgrest["building_config"].update(
            value = config
        ) {
            filter {
                eq("id", config.buildingId)
            }
        }
    }

    suspend fun uploadAudioProof(file: File, buildingId: String): String? {
        // Skip file uploads in free tier without Firebase
        // Supabase storage could be implemented here
        return null
    }

    suspend fun flagNoise(flag: NoiseFlag, buildingId: String, audioUrl: String? = null) {
        // We use an anonymous data class or a map for insertion to avoid UUID auto-gen issues
        @kotlinx.serialization.Serializable
        data class InsertFlag(
            @kotlinx.serialization.SerialName("flagger_room_id") val flaggerRoomId: String,
            @kotlinx.serialization.SerialName("building_id") val buildingId: String,
            val timestamp: Long,
            @kotlinx.serialization.SerialName("time_window") val timeWindow: Long,
            @kotlinx.serialization.SerialName("audio_url") val audioUrl: String?
        )
        
        postgrest["flags"].insert(InsertFlag(
            flaggerRoomId = flag.flaggerRoomId,
            buildingId = buildingId,
            timestamp = flag.timestamp,
            timeWindow = flag.timeWindow,
            audioUrl = audioUrl
        ))
            
        // TRIGGER SIMULATED BACKEND
        triggerSimulatedConsensus(buildingId, flag.timeWindow)
    }

    private suspend fun triggerSimulatedConsensus(buildingId: String, timeWindow: Long) {
        try {
            val config = getBuildingConfig(buildingId)
            
            val flags = postgrest["flags"].select {
                filter {
                    eq("building_id", buildingId)
                    eq("time_window", timeWindow)
                }
            }.decodeList<NoiseFlag>()
            
            if (flags.size >= config.consensusThreshold) {
                // In a real app, we'd find the culprit room via proximity logic.
                // For demo, we'll pick a "culprit" room (maybe 102 if 101 flagged).
                val culpritRoomId = "102" 
                
                // Capture first flagger for filtering logic
                val firstFlagger = flags.firstOrNull()?.flaggerRoomId

                // Check if incident already exists for this window
                val existing = postgrest["confirmed_incidents"].select {
                    filter {
                        eq("room_id", culpritRoomId)
                        eq("timestamp", timeWindow)
                    }
                }.decodeList<ConfirmedIncident>()
                
                if (existing.isEmpty()) {
                    // Check for Warden Escalation (e.g. if this is the 5th incident for this room)
                    val pastIncidents = postgrest["confirmed_incidents"].select {
                        filter {
                            eq("room_id", culpritRoomId)
                        }
                    }.decodeList<ConfirmedIncident>()
                    
                    val wardenThreshold = config.escalationTiers.lastOrNull() ?: 5
                    val shouldEscalate = pastIncidents.size >= wardenThreshold
                    
                    @kotlinx.serialization.Serializable
                    data class InsertIncident(
                        @kotlinx.serialization.SerialName("room_id") val roomId: String,
                        @kotlinx.serialization.SerialName("flagger_room_id") val flaggerRoomId: String?,
                        val timestamp: Long,
                        val severity: Int,
                        @kotlinx.serialization.SerialName("is_warden_escalated") val isWardenEscalated: Boolean
                    )
                    
                    postgrest["confirmed_incidents"].insert(InsertIncident(
                        roomId = culpritRoomId,
                        flaggerRoomId = firstFlagger,
                        timestamp = timeWindow,
                        severity = (flags.size - config.consensusThreshold + 1).coerceIn(1, 4),
                        isWardenEscalated = shouldEscalate
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RoomRepository", "Simulated Backend Error: ${e.message}")
        }
    }

    suspend fun getConfirmedIncidents(roomId: String): List<ConfirmedIncident> {
        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -AppConstants.INCIDENT_HISTORY_DAYS)
        }.time.time

        return try {
            postgrest["confirmed_incidents"].select {
                filter {
                    eq("room_id", roomId)
                    gt("timestamp", thirtyDaysAgo)
                }
                order("timestamp", Order.DESCENDING)
                limit(30)
            }.decodeList<ConfirmedIncident>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getIncidentsByFlagger(roomId: String): List<ConfirmedIncident> {
        return try {
            val flags = postgrest["flags"].select {
                filter {
                    eq("flagger_room_id", roomId)
                }
                order("timestamp", Order.DESCENDING)
                limit(30)
            }.decodeList<NoiseFlag>()
            
            if (flags.isEmpty()) return emptyList()

            val timeWindows = flags.map { it.timeWindow }
            
            val confirmedTimeWindows = try {
                postgrest["confirmed_incidents"].select {
                    filter {
                        isIn("timestamp", timeWindows)
                    }
                }.decodeList<ConfirmedIncident>().map { it.timestamp }.toSet()
            } catch(e: Exception) {
                emptySet<Long>()
            }
            
            // Map flags to ConfirmedIncident view for UI
            flags.map { flag ->
                val status = if (confirmedTimeWindows.contains(flag.timeWindow)) "Confirmed" else "Waiting"
                
                ConfirmedIncident(
                    id = "",
                    roomId = "Unknown",
                    flaggerRoomId = flag.flaggerRoomId,
                    timestamp = flag.timestamp,
                    severity = 1,
                    status = status
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEscalatedIncidents(buildingId: String): List<ConfirmedIncident> {
        return try {
            postgrest["confirmed_incidents"].select {
                filter {
                    eq("is_warden_escalated", true)
                }
                order("timestamp", Order.DESCENDING)
                limit(30)
            }.decodeList<ConfirmedIncident>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
