package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.HarvestSchedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class HarvestRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = AuthRepository()
    private val harvestCollection = db.collection("harvest_schedules")

    suspend fun addHarvest(harvest: HarvestSchedule): Result<String> {
        return try {
            val docRef = harvestCollection.document()
            val harvestWithId = harvest.copy(id = docRef.id)
            docRef.set(harvestWithId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserHarvests(): Result<List<HarvestSchedule>> {
        return try {
            // Call getUserProfile() and handle the Result
            val userResult = auth.getUserProfile()
            val user = userResult.getOrElse { return Result.failure(it) }  // If failed, return immediately

            val userId = user.uid
            val region = user.region

            val snapshot = harvestCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .whereEqualTo("region", region)
                .orderBy("harvestStartDate", Query.Direction.ASCENDING)
                .get()
                .await()

            val harvests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(HarvestSchedule::class.java)
            }

            Result.success(harvests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getRegionHarvests(region: String): Result<List<HarvestSchedule>> {
        return try {
            val snapshot = harvestCollection
                .whereEqualTo("region", region)
                .whereEqualTo("status", "active")
                .orderBy("harvestStartDate", Query.Direction.ASCENDING)
                .get()
                .await()

            val harvests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(HarvestSchedule::class.java)
            }
            Result.success(harvests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHarvest(harvest: HarvestSchedule): Result<Unit> {
        return try {
            harvestCollection.document(harvest.id)
                .set(harvest.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHarvest(harvestId: String): Result<Unit> {
        return try {
            harvestCollection.document(harvestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHarvestById(harvestId: String): Result<HarvestSchedule> {
        return try {
            val doc = harvestCollection.document(harvestId).get().await()
            val harvest = doc.toObject(HarvestSchedule::class.java)
            if (harvest != null) {
                Result.success(harvest)
            } else {
                Result.failure(Exception("Harvest not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}