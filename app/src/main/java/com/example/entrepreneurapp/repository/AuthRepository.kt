package com.example.entrepreneurapp.repository

import com.example.entrepreneurapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Get current user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Register new user
    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        region: String

    ): Result<User> {
        return try {
            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Create user object
                val user = User(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    phone = phone,
                    region = region,
                    avatar = "👤",
                    joinedDate = System.currentTimeMillis()
                )

                // Save to Firestore
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(user.toMap())
                    .await()

                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login user
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Sign in with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Get user data from Firestore
                val documentSnapshot = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val user = documentSnapshot.toObject(User::class.java)

                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data not found"))
                }
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<User> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not logged in"))

            val doc = firestore.collection("users").document(userId).get().await()
            val user: User? = doc.toObject(User::class.java)  // 👈 Explicit type annotation

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Logout
    fun logout() {
        auth.signOut()
    }
}