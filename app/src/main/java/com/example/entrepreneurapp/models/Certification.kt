package com.example.entrepreneurapp.models

data class Certification(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // MyGAP, Organic, HACCP, Halal, ISO 22000, GlobalGAP
    val certificateNumber: String = "",
    val issuedDate: Long = 0L,
    val expiryDate: Long = 0L,
    val issuingBody: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "type" to type,
            "certificateNumber" to certificateNumber,
            "issuedDate" to issuedDate,
            "expiryDate" to expiryDate,
            "issuingBody" to issuingBody,
            "notes" to notes,
            "createdAt" to createdAt
        )
    }

    // Calculate days until expiry
    fun daysUntilExpiry(): Long {
        val currentTime = System.currentTimeMillis()
        val diff = expiryDate - currentTime
        return diff / (1000 * 60 * 60 * 24) // Convert milliseconds to days
    }

    // Get expiry status
    fun getExpiryStatus(): ExpiryStatus {
        val days = daysUntilExpiry()
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 7 -> ExpiryStatus.CRITICAL
            days <= 30 -> ExpiryStatus.EXPIRING_VERY_SOON
            days <= 90 -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.VALID
        }
    }

    // Get status color
    fun getStatusColor(): String {
        return when (getExpiryStatus()) {
            ExpiryStatus.EXPIRED -> "#D32F2F" // Red
            ExpiryStatus.CRITICAL -> "#F57C00" // Orange
            ExpiryStatus.EXPIRING_VERY_SOON -> "#FFA000" // Amber
            ExpiryStatus.EXPIRING_SOON -> "#FBC02D" // Yellow
            ExpiryStatus.VALID -> "#388E3C" // Green
        }
    }

    // Get status icon
    fun getStatusIcon(): String {
        return when (getExpiryStatus()) {
            ExpiryStatus.EXPIRED -> "🔴"
            ExpiryStatus.CRITICAL -> "🟠"
            ExpiryStatus.EXPIRING_VERY_SOON -> "🟠"
            ExpiryStatus.EXPIRING_SOON -> "🟡"
            ExpiryStatus.VALID -> "🟢"
        }
    }

    // Get status text
    fun getStatusText(): String {
        val days = daysUntilExpiry()
        return when {
            days < 0 -> "Expired ${-days} days ago"
            days == 0L -> "Expires today!"
            days <= 7 -> "Critical: $days days remaining"
            days <= 30 -> "Expiring in $days days"
            days <= 90 -> "Expiring in $days days"
            else -> "Valid ($days days remaining)"
        }
    }
}

enum class ExpiryStatus {
    VALID,
    EXPIRING_SOON,
    EXPIRING_VERY_SOON,
    CRITICAL,
    EXPIRED
}