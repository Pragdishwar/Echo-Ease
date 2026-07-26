package com.echoease.app.data.repository

import com.echoease.app.data.model.Building
import com.echoease.app.data.model.BuildingConfig
import com.echoease.app.data.model.ConfirmedIncident
import com.echoease.app.data.model.NoiseFlag
import com.echoease.app.data.model.Room
import com.echoease.app.data.model.RoomAdjacency
import com.echoease.app.data.model.UserProfile
import com.echoease.app.util.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Calendar
import javax.inject.Singleton

@Singleton
class RoomRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    suspend fun getAllBuildings(): List<Building> {
        if (AppConstants.USE_MOCK_DATA) {
            return listOf(
                Building("b1", "Echo Hostel", "123 Main St", "Tech City"),
                Building("b2", "Ease Apartments", "456 Oak Rd", "Nature City")
            )
        }
        return try {
            firestore.collection("buildings")
                .get()
                .await()
                .toObjects(Building::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRoomsByBuilding(buildingId: String): List<Room> {
        if (AppConstants.USE_MOCK_DATA) {
            return listOf(
                Room("r1", "Room 101", 1),
                Room("r2", "Room 102", 1),
                Room("r3", "Room 201", 2)
            )
        }
        return try {
            firestore.collection("rooms")
                .whereEqualTo("buildingId", buildingId)
                .get()
                .await()
                .toObjects(Room::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        firestore.collection("users")
            .document(profile.uid)
            .set(profile)
            .await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        if (AppConstants.USE_MOCK_DATA) {
            return UserProfile(uid, "r1", "b1", "user@example.com", "resident")
        }
        return try {
            firestore.collection("users")
                .document(uid)
                .get()
                .await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getBuildingConfig(buildingId: String): BuildingConfig {
        return try {
            firestore.collection("buildings")
                .document(buildingId)
                .get()
                .await()
                .toObject(BuildingConfig::class.java) ?: BuildingConfig(buildingId)
        } catch (e: Exception) {
            BuildingConfig(buildingId)
        }
    }

    suspend fun updateBuildingConfig(config: BuildingConfig) {
        firestore.collection("buildings")
            .document(config.buildingId)
            .set(config)
            .await()
    }

    suspend fun uploadAudioProof(file: File, buildingId: String): String {
        val storageRef = storage.reference.child("audio_proofs/$buildingId/${System.currentTimeMillis()}.m4a")
        storageRef.putFile(android.net.Uri.fromFile(file)).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun flagNoise(flag: NoiseFlag, buildingId: String, audioUrl: String? = null) {
        firestore.collection("flags")
            .add(mapOf(
                "flaggerRoomId" to flag.flaggerRoomId,
                "buildingId" to buildingId,
                "timestamp" to com.google.firebase.Timestamp(flag.timestamp / 1000, (flag.timestamp % 1000 * 1000000).toInt()),
                "timeWindow" to flag.timeWindow,
                "audioUrl" to audioUrl
            ))
            .await()
            
        // TRIGGER SIMULATED BACKEND
        triggerSimulatedConsensus(buildingId, flag.timeWindow)
    }

    private suspend fun triggerSimulatedConsensus(buildingId: String, timeWindow: Long) {
        try {
            // Get config
            val config = getBuildingConfig(buildingId)
            
            // Get all flags for this window
            val flags = firestore.collection("flags")
                .whereEqualTo("buildingId", buildingId)
                .whereEqualTo("timeWindow", timeWindow)
                .get()
                .await()
            
            if (flags.size() >= config.consensusThreshold) {
                // In a real app, we'd find the culprit room via proximity logic.
                // For demo, we'll pick a "culprit" room (maybe r2 if r1 flagged).
                val culpritRoomId = "r2" 
                
                // Collect any audio URLs as proof
                val proofs = flags.documents.mapNotNull { it.getString("audioUrl") }
                
                // Capture first flagger for filtering logic
                val firstFlagger = flags.documents.firstOrNull()?.getString("flaggerRoomId")

                // Check if incident already exists for this window
                val existing = firestore.collection("confirmedIncidents")
                    .whereEqualTo("roomId", culpritRoomId)
                    .whereEqualTo("timestamp", com.google.firebase.Timestamp(timeWindow / 1000, 0))
                    .get()
                    .await()
                
                if (existing.isEmpty) {
                    // Check for Warden Escalation (e.g. if this is the 5th incident for this room)
                    val pastIncidents = firestore.collection("confirmedIncidents")
                        .whereEqualTo("roomId", culpritRoomId)
                        .get()
                        .await()
                    
                    val wardenThreshold = config.escalationTiers.lastOrNull() ?: 5
                    val shouldEscalate = pastIncidents.size() >= wardenThreshold

                    firestore.collection("confirmedIncidents").add(mapOf(
                        "roomId" to culpritRoomId,
                        "flaggerRoomId" to firstFlagger,
                        "timestamp" to com.google.firebase.Timestamp(timeWindow / 1000, 0),
                        "severity" to (flags.size() - config.consensusThreshold + 1).coerceIn(1, 4),
                        "audioProofUrl" to proofs.firstOrNull(),
                        "isWardenEscalated" to shouldEscalate
                    )).await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RoomRepository", "Simulated Backend Error: ${e.message}")
        }
    }

    suspend fun getConfirmedIncidents(roomId: String): List<ConfirmedIncident> {
        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -AppConstants.INCIDENT_HISTORY_DAYS)
        }.time

        return try {
            firestore.collection("confirmedIncidents")
                .whereEqualTo("roomId", roomId)
                .whereGreaterThan("timestamp", com.google.firebase.Timestamp(thirtyDaysAgo))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val ts = data["timestamp"] as? com.google.firebase.Timestamp
                    ConfirmedIncident(
                        id = doc.id,
                        roomId = data["roomId"] as? String ?: "",
                        flaggerRoomId = data["flaggerRoomId"] as? String,
                        timestamp = ts?.toDate()?.time ?: 0L,
                        severity = (data["severity"] as? Long)?.toInt() ?: 1,
                        audioProofUrl = data["audioProofUrl"] as? String,
                        isWardenEscalated = data["isWardenEscalated"] as? Boolean ?: false
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getIncidentsByFlagger(roomId: String): List<ConfirmedIncident> {
        return try {
            firestore.collection("flags")
                .whereEqualTo("flaggerRoomId", roomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val ts = data["timestamp"] as? com.google.firebase.Timestamp
                    val timeWindow = data["timeWindow"] as? Long
                    
                    var status = "Waiting"
                    if (timeWindow != null) {
                        val confirmed = firestore.collection("confirmedIncidents")
                            .whereEqualTo("timestamp", com.google.firebase.Timestamp(timeWindow / 1000, 0))
                            .get()
                            .await()
                        if (!confirmed.isEmpty) {
                            status = "Confirmed"
                        }
                    }

                    ConfirmedIncident(
                        id = doc.id,
                        roomId = "Unknown",
                        flaggerRoomId = data["flaggerRoomId"] as? String,
                        timestamp = ts?.toDate()?.time ?: 0L,
                        severity = 1,
                        audioProofUrl = data["audioUrl"] as? String,
                        isWardenEscalated = false,
                        status = status
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
