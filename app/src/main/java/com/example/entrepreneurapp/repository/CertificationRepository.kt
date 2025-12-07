package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.Certification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CertificationRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val certificationsCollection = firestore.collection("certifications")

    // Add new certification
    suspend fun addCertification(certification: Certification): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Generate new certification ID
            val certificationId = certificationsCollection.document().id

            // Create certification with user ID
            val certificationData = certification.copy(
                id = certificationId,
                userId = currentUser.uid
            )

            // Save to Firestore
            certificationsCollection.document(certificationId)
                .set(certificationData.toMap())
                .await()

            Result.success(certificationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all certifications for current user
    suspend fun getUserCertifications(userId: String?=null): Result<List<Certification>> {
        return try {
            val uid = userId?:auth.currentUser?.uid
            if (uid == null) {
                return Result.failure(Exception("User not logged in"))
            }

            val querySnapshot = certificationsCollection
                .whereEqualTo("userId", uid)
                .orderBy("expiryDate", Query.Direction.ASCENDING)
                .get()
                .await()

            val certifications = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Certification(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        type = doc.getString("type") ?: "",
                        certificateNumber = doc.getString("certificateNumber") ?: "",
                        issuedDate = (doc.get("issuedDate") as? Number)?.toLong() ?: 0L,
                        expiryDate = (doc.get("expiryDate") as? Number)?.toLong() ?: 0L,
                        issuingBody = doc.getString("issuingBody") ?: "",
                        notes = doc.getString("notes") ?: "",
                        createdAt = (doc.get("createdAt") as? Number)?.toLong() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(certifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update existing certification
    suspend fun updateCertification(certification: Certification): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not logged in"))
            }

            // Update in Firestore
            certificationsCollection.document(certification.id)
                .set(certification.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete certification
    suspend fun deleteCertification(certificationId: String): Result<Unit> {
        return try {
            certificationsCollection.document(certificationId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get certifications expiring soon (within 90 days)
    suspend fun getCertificationsExpiringSoon(): Result<List<Certification>> {
        return try {
            val result = getUserCertifications()
            result.fold(
                onSuccess = { certifications ->
                    val expiringSoon = certifications.filter { cert ->
                        val days = cert.daysUntilExpiry()
                        days in 0..90
                    }
                    Result.success(expiringSoon)
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}