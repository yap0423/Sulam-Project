package com.example.entrepreneurapp.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar: String = "👤",
    val region: String = "",
    val joinedDate: Long = System.currentTimeMillis(),
    // Profile fields (empty initially - added later)
    val businessName: String = "",
    val businessType: String = ""
) {
    // Convert to HashMap for Firestore
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "avatar" to avatar,
            "joinedDate" to joinedDate,
            "businessName" to businessName,
            "businessType" to businessType,
            "region" to region
        )
    }
}
