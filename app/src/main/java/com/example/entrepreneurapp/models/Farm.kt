package com.example.entrepreneurapp.models

data class Farm(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val location: String = "",
    val gpsLatitude: String = "",
    val gpsLongitude: String = "",
    val totalSize: Double = 0.0,
    val farmerType: String = "",
    val varieties: List<PineappleVariety> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "location" to location,
            "gpsLatitude" to gpsLatitude,
            "gpsLongitude" to gpsLongitude,
            "totalSize" to totalSize,
            "farmerType" to farmerType,
            "varieties" to varieties.map { it.toMap() },
            "createdAt" to createdAt
        )
    }
}

data class PineappleVariety(
    val variety: String = "",
    val areaSize: Double = 0.0
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "variety" to variety,
            "areaSize" to areaSize
        )
    }
}
