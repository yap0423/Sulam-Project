package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.Announcement
import com.example.entrepreneurapp.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AnnouncementRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val announcementsCollection = firestore.collection("announcements")
    private val commentsCollection = firestore.collection("comments")

    // Create announcement
    suspend fun createAnnouncement(announcement: Announcement): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Generate new announcement ID
            val announcementId = announcementsCollection.document().id

            // Get user data
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "User"
            val userAvatar = userDoc.getString("avatar") ?: "👤"

            // Create announcement with user ID and info
            val announcementData = announcement.copy(
                id = announcementId,
                userId = currentUser.uid,
                userName = userName,
                userAvatar = userAvatar
            )

            // Save to Firestore
            announcementsCollection.document(announcementId)
                .set(announcementData.toMap())
                .await()

            Result.success(announcementId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all announcements
    suspend fun getAllAnnouncements(): Result<List<Announcement>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val querySnapshot = announcementsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val announcements = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Announcement(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userAvatar = doc.getString("userAvatar") ?: "👤",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        likesCount = (doc.get("likesCount") as? Number)?.toInt() ?: 0,
                        likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                        commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0,
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(announcements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAnnouncements(): Result<List<Announcement>> {
        return try {
            // Get current user
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            val userId = currentUser.uid

            // Query announcements where userId matches current user
            val querySnapshot = announcementsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            // Map documents to Announcement objects
            val announcements = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Announcement(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userAvatar = doc.getString("userAvatar") ?: "👤",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        likesCount = (doc.get("likesCount") as? Number)?.toInt() ?: 0,
                        likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                        commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0,
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(announcements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get announcements by category
    suspend fun getAnnouncementsByCategory(category: String): Result<List<Announcement>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val querySnapshot = announcementsCollection
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val announcements = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Announcement(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userAvatar = doc.getString("userAvatar") ?: "👤",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        likesCount = (doc.get("likesCount") as? Number)?.toInt() ?: 0,
                        likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                        commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0,
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(announcements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get single announcement
    suspend fun getAnnouncement(announcementId: String): Result<Announcement> {
        return try {
            val doc = announcementsCollection.document(announcementId).get().await()

            val announcement = Announcement(
                id = doc.getString("id") ?: "",
                userId = doc.getString("userId") ?: "",
                userName = doc.getString("userName") ?: "",
                userAvatar = doc.getString("userAvatar") ?: "👤",
                title = doc.getString("title") ?: "",
                content = doc.getString("content") ?: "",
                category = doc.getString("category") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                likesCount = (doc.get("likesCount") as? Number)?.toInt() ?: 0,
                likedBy = doc.get("likedBy") as? List<String> ?: emptyList(),
                commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0,
                createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
            )

            Result.success(announcement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update announcement
    suspend fun updateAnnouncement(announcement: Announcement): Result<Unit> {
        return try {
            announcementsCollection.document(announcement.id)
                .set(announcement.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete announcement
    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        return try {
            // Delete announcement
            announcementsCollection.document(announcementId)
                .delete()
                .await()

            // Delete all comments for this announcement
            val comments = commentsCollection
                .whereEqualTo("announcementId", announcementId)
                .get()
                .await()

            comments.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Like announcement
    suspend fun likeAnnouncement(announcementId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val announcementRef = announcementsCollection.document(announcementId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(announcementRef)
                val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()

                if (!likedBy.contains(currentUser.uid)) {
                    // Add like
                    transaction.update(announcementRef, "likedBy", FieldValue.arrayUnion(currentUser.uid))
                    transaction.update(announcementRef, "likesCount", FieldValue.increment(1))
                } else {
                    // Remove like
                    transaction.update(announcementRef, "likedBy", FieldValue.arrayRemove(currentUser.uid))
                    transaction.update(announcementRef, "likesCount", FieldValue.increment(-1))
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add comment
    suspend fun addComment(comment: Comment): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Get user data
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "User"
            val userAvatar = userDoc.getString("avatar") ?: "👤"

            // Generate comment ID
            val commentId = commentsCollection.document().id

            val commentData = comment.copy(
                id = commentId,
                userId = currentUser.uid,
                userName = userName,
                userAvatar = userAvatar
            )

            // Save comment
            commentsCollection.document(commentId)
                .set(commentData.toMap())
                .await()

            // Increment comments count
            announcementsCollection.document(comment.announcementId)
                .update("commentsCount", FieldValue.increment(1))
                .await()

            Result.success(commentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get comments for announcement
    suspend fun getComments(announcementId: String): Result<List<Comment>> {
        return try {
            val querySnapshot = commentsCollection
                .whereEqualTo("announcementId", announcementId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Comment(
                        id = doc.getString("id") ?: "",
                        announcementId = doc.getString("announcementId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userAvatar = doc.getString("userAvatar") ?: "👤",
                        content = doc.getString("content") ?: "",
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete comment
    suspend fun deleteComment(commentId: String, announcementId: String): Result<Unit> {
        return try {
            // Delete comment
            commentsCollection.document(commentId)
                .delete()
                .await()

            // Decrement comments count
            announcementsCollection.document(announcementId)
                .update("commentsCount", FieldValue.increment(-1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}