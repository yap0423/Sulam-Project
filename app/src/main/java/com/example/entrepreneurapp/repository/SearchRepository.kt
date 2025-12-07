package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SearchRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun search(query: String): Result<GroupedSearchResults> {
        return try {
            val queryLower = query.lowercase()

            // Search people
            val peopleSnapshot = db.collection("users").get().await()
            val people = peopleSnapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null && (
                            user.name.lowercase().contains(queryLower) ||
                                    user.businessName.lowercase().contains(queryLower) ||
                                    user.email.lowercase().contains(queryLower) ||
                                    user.region.lowercase().contains(queryLower)
                            )) {
                    SearchResult.PersonResult(
                        userId = user.uid,
                        name = user.name,
                        avatar = user.avatar,
                        businessName = user.businessName,
                        region = user.region,
                        email = user.email,
                        phone = user.phone
                    )
                } else null
            }

            // Search farms
            val farmsSnapshot = db.collection("farms").get().await()
            val farms = farmsSnapshot.documents.mapNotNull { doc ->
                val farm = doc.toObject(Farm::class.java)
                if (farm != null && (
                            farm.name.lowercase().contains(queryLower) ||
                                    farm.location.lowercase().contains(queryLower)
                            )) {

                    SearchResult.FarmResult(
                        farmId = farm.id,
                        farmName = farm.name,
                        location = farm.location,
                        ownerId = farm.userId,       // <-- Return owner ID instead of name
                        totalSize = farm.totalSize
                    )
                } else null
            }

// Search businesses
            val businessesSnapshot = db.collection("businesses").get().await()
            val businesses = businessesSnapshot.documents.mapNotNull { doc ->
                val business = doc.toObject(Business::class.java)
                if (business != null && (
                            business.name.lowercase().contains(queryLower) ||
                                    business.type.lowercase().contains(queryLower) ||
                                    business.location.lowercase().contains(queryLower)
                            )) {

                    SearchResult.BusinessResult(
                        businessId = business.id,
                        businessName = business.name,
                        type = business.type,
                        location = business.location,
                        ownerId = business.userId     // <-- Return owner ID instead of name
                    )
                } else null
            }


            // Search announcements
            val announcementsSnapshot = db.collection("announcements").get().await()
            val announcements = announcementsSnapshot.documents.mapNotNull { doc ->
                val announcement = doc.toObject(Announcement::class.java)
                if (announcement != null && (
                            announcement.title.lowercase().contains(queryLower) ||
                                    announcement.content.lowercase().contains(queryLower) ||
                                    announcement.category.lowercase().contains(queryLower)
                            )) {
                    SearchResult.AnnouncementResult(announcement)
                } else null
            }

            val results = GroupedSearchResults(
                people = people,
                farms = farms,
                businesses = businesses,
                announcements = announcements
            )

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun searchByUserId(userId: String): Result<GroupedSearchResults> {
        return try {

            // 1. Find user
            val userSnapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            val user = userSnapshot.toObject(User::class.java)

            val people = if (user != null) {
                listOf(
                    SearchResult.PersonResult(
                        userId = user.uid,
                        name = user.name,
                        avatar = user.avatar,
                        businessName = user.businessName,
                        region = user.region,
                        email = user.email,
                        phone = user.phone
                    )
                )
            } else emptyList()


            // 2. Find all farms with this userId
            val farmsSnapshot = db.collection("farms")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val farms = farmsSnapshot.documents.mapNotNull { doc ->
                val farm = doc.toObject(Farm::class.java)
                farm?.let {
                    SearchResult.FarmResult(
                        farmId = farm.id,
                        farmName = farm.name,
                        location = farm.location,
                        ownerId = farm.userId,
                        totalSize = farm.totalSize
                    )
                }
            }


            // 3. Find all businesses with this userId
            val businessesSnapshot = db.collection("businesses")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val businesses = businessesSnapshot.documents.mapNotNull { doc ->
                val business = doc.toObject(Business::class.java)
                business?.let {
                    SearchResult.BusinessResult(
                        businessId = business.id,
                        businessName = business.name,
                        type = business.type,
                        location = business.location,
                        ownerId = business.userId
                    )
                }
            }

            // Return all grouped results
            Result.success(
                GroupedSearchResults(
                    people = people,
                    farms = farms,
                    businesses = businesses
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}