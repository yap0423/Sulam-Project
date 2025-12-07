package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.Business
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BusinessRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val businessesCollection = firestore.collection("businesses")

    // Add new business
    suspend fun addBusiness(business: Business): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Generate new business ID
            val businessId = businessesCollection.document().id

            // Create business with user ID
            val businessData = business.copy(
                id = businessId,
                userId = currentUser.uid
            )

            // Save to Firestore
            businessesCollection.document(businessId)
                .set(businessData.toMap())
                .await()

            Result.success(businessId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all businesses for current user
    suspend fun getUserBusinesses(userId:String?=null): Result<List<Business>> {
        return try {
            val uid = userId?:auth.currentUser?.uid
            if (uid == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val querySnapshot = businessesCollection
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val businesses = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Business(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "",
                        location = doc.getString("location") ?: "",
                        gpsLatitude = doc.getString("gpsLatitude") ?: "",
                        gpsLongitude = doc.getString("gpsLongitude") ?: "",
                        phone = doc.getString("phone") ?: "",
                        description = doc.getString("description") ?: "",
                        operatingHours = doc.getString("operatingHours") ?: "",
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(businesses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update existing business
    suspend fun updateBusiness(business: Business): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Update in Firestore
            businessesCollection.document(business.id)
                .set(business.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete business
    suspend fun deleteBusiness(businessId: String): Result<Unit> {
        return try {
            businessesCollection.document(businessId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}