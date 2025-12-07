package com.example.entrepreneurapp.models

import com.google.firebase.Timestamp

data class HarvestSchedule(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "👤",
    val cropType: String = "",
    val variety: String = "",
    val plantedDate: Timestamp = Timestamp.now(),
    val estimatedYield: Double = 0.0, // in kg
    val harvestStartDate: Timestamp = Timestamp.now(),
    val harvestEndDate: Timestamp = Timestamp.now(),
    val region: String = "",
    val status: String = "active", // active, completed, cancelled
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "cropType" to cropType,
            "variety" to variety,
            "plantedDate" to plantedDate,
            "estimatedYield" to estimatedYield,
            "harvestStartDate" to harvestStartDate,
            "harvestEndDate" to harvestEndDate,
            "region" to region,
            "status" to status,
            "notes" to notes,
            "createdAt" to createdAt
        )
    }

    fun getHarvestPeriodString(): String {
        val startDate = harvestStartDate.toDate()
        val endDate = harvestEndDate.toDate()
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return "${formatter.format(startDate)} - ${formatter.format(endDate)}"
    }

    fun getDaysUntilHarvest(): Long {
        val now = System.currentTimeMillis()
        val harvestTime = harvestStartDate.toDate().time
        return (harvestTime - now) / (1000 * 60 * 60 * 24)
    }
}

data class GroupMember(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "👤",
    val region: String = "",
    val businessName: String = ""
)

data class HarvestConflict(
    val date: Timestamp = Timestamp.now(),
    val totalYield: Double = 0.0,
    val riskLevel: String = "normal", // normal, medium, high
    val farmersAffected: List<String> = emptyList(), // List of user IDs
    val schedules: List<HarvestSchedule> = emptyList()
) {
    fun getRiskColor(): String {
        return when (riskLevel) {
            "high" -> "#D32F2F"
            "medium" -> "#FFA000"
            else -> "#388E3C"
        }
    }

    fun getRiskEmoji(): String {
        return when (riskLevel) {
            "high" -> "🔴"
            "medium" -> "🟡"
            else -> "🟢"
        }
    }

    fun getDateString(): String {
        val formatter = java.text.SimpleDateFormat("EEEE, MMMM dd", java.util.Locale.getDefault())
        return formatter.format(date.toDate())
    }
}

data class ChatMessage(
    val id: String = "",
    val conflictDate: String = "", // Date of the conflict (used to group messages)
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "👤",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isResolution: Boolean = false // Special message type for resolved conflicts
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "id" to id,
            "conflictDate" to conflictDate,
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "message" to message,
            "timestamp" to timestamp,
            "isResolution" to isResolution
        )
    }

    fun getTimeString(): String {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(timestamp.toDate())
    }
}