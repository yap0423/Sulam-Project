package com.example.entrepreneurapp.models

data class Announcement(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "👤",
    val title: String = "",
    val content: String = "",
    val category: String = "",
    val imageUrl: String = "", // Single image for now
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(), // List of user IDs who liked
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "title" to title,
            "content" to content,
            "category" to category,
            "imageUrl" to imageUrl,
            "likesCount" to likesCount,
            "likedBy" to likedBy,
            "commentsCount" to commentsCount,
            "createdAt" to createdAt
        )
    }

    // Check if current user liked this announcement
    fun isLikedBy(userId: String): Boolean {
        return likedBy.contains(userId)
    }

    // Get time ago string
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            years > 0 -> "${years}y ago"
            months > 0 -> "${months}mo ago"
            weeks > 0 -> "${weeks}w ago"
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    // Get category emoji
    fun getCategoryEmoji(): String {
        return when (category) {
            "General Updates" -> "🌾"
            "Market Prices" -> "💰"
            "Equipment & Tools" -> "🚜"
            "Growing Tips" -> "🌱"
            "Collaboration" -> "🤝"
            "Events & Workshops" -> "📢"
            "Alerts & Warnings" -> "⚠️"
            "Success Stories" -> "🎉"
            else -> "📢"
        }
    }

    // Get category color
    fun getCategoryColor(): String {
        return when (category) {
            "General Updates" -> "#16A34A"
            "Market Prices" -> "#D97706"
            "Equipment & Tools" -> "#7C3AED"
            "Growing Tips" -> "#059669"
            "Collaboration" -> "#0284C7"
            "Events & Workshops" -> "#DC2626"
            "Alerts & Warnings" -> "#EA580C"
            "Success Stories" -> "#EC4899"
            else -> "#6B7280"
        }
    }
}

data class Comment(
    val id: String = "",
    val announcementId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "👤",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "announcementId" to announcementId,
            "userId" to userId,
            "userName" to userName,
            "userAvatar" to userAvatar,
            "content" to content,
            "createdAt" to createdAt
        )
    }

    // Get time ago string
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
}

enum class AnnouncementCategory(val displayName: String) {
    GENERAL("General Updates"),
    MARKET_PRICES("Market Prices"),
    EQUIPMENT("Equipment & Tools"),
    GROWING_TIPS("Growing Tips"),
    COLLABORATION("Collaboration"),
    EVENTS("Events & Workshops"),
    ALERTS("Alerts & Warnings"),
    SUCCESS("Success Stories")
}