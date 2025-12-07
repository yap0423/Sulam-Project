package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.Farm
import  com.example.entrepreneurapp.models.PineappleVariety
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FarmRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val farmsCollection = firestore.collection("farms")

    // Add new farm
    suspend fun addFarm(farm: Farm): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Generate new farm ID
            val farmId = farmsCollection.document().id

            // Create farm with user ID
            val farmData = farm.copy(
                id = farmId,
                userId = currentUser.uid
            )

            // Save to Firestore
            farmsCollection.document(farmId)
                .set(farmData.toMap())
                .await()

            Result.success(farmId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all farms for current user
    suspend fun getUserFarms(userId: String? = null): Result<List<Farm>> {
        return try {
            val uid = userId ?: auth.currentUser?.uid
            if (uid == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val querySnapshot = farmsCollection
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val farms = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val varietiesList = (doc.get("varieties") as? List<*>)?.mapNotNull { varietyMap ->
                        val map = varietyMap as? Map<*, *>
                        if (map != null) {
                            PineappleVariety(
                                variety = map["variety"] as? String ?: "",
                                areaSize = (map["areaSize"] as? Number)?.toDouble() ?: 0.0
                            )
                        } else null
                    } ?: emptyList()

                    Farm(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        location = doc.getString("location") ?: "",
                        gpsLatitude = doc.getString("gpsLatitude") ?: "",
                        gpsLongitude = doc.getString("gpsLongitude") ?: "",
                        totalSize = (doc.get("totalSize") as? Number)?.toDouble() ?: 0.0,
                        farmerType = doc.getString("farmerType") ?: "",
                        varieties = varietiesList,
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(farms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Delete farm
    suspend fun deleteFarm(farmId: String): Result<Unit> {
        return try {
            farmsCollection.document(farmId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update existing farm
    suspend fun updateFarm(farm: Farm): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Update in Firestore
            farmsCollection.document(farm.id)
                .set(farm.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}