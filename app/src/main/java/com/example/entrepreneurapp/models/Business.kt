package com.example.entrepreneurapp.models
data class Business(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = "", // Shop, Stall, Outlet, Processing Plant
    val location: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val phone: String = "",
    val description: String = "",
    val operatingHours: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "type" to type,
            "location" to location,
            "gpsLatitude" to gpsLatitude,
            "gpsLongitude" to gpsLongitude,
            "phone" to phone,
            "description" to description,
            "operatingHours" to operatingHours,
            "createdAt" to createdAt
        )
    }
}