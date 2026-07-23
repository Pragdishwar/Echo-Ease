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
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Singleton

@Singleton
class RoomRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

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

    suspend fun flagNoise(flag: NoiseFlag, buildingId: String) {
        firestore.collection("flags")
            .add(mapOf(
                "flaggerRoomId" to flag.flaggerRoomId,
                "buildingId" to buildingId,
                "timestamp" to com.google.firebase.Timestamp(flag.timestamp / 1000, (flag.timestamp % 1000 * 1000000).toInt()),
                "timeWindow" to flag.timeWindow
            ))
            .await()
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
                        timestamp = ts?.toDate()?.time ?: 0L,
                        severity = (data["severity"] as? Long)?.toInt() ?: 1
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
