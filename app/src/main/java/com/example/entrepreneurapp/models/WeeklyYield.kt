package com.example.entrepreneurapp.models

import android.content.Context
import android.widget.Toast

data class WeeklyYield(
    val weekStart: String = "",
    val weekEnd: String = "",
    val totalYield: Double = 0.0,
    val riskLevel: String = "NORMAL",
    val farmerCount: Int = 0
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


    fun getYieldPercentage(region: String): Int {

        val max = getWeeklyAvg(region)
        val percentage = ((totalYield / max) * 100)
            .toInt()
            .coerceIn(0, 100)

        return percentage
    }

    fun getWeeklyAvg(region: String): Float {
        return when (region) {
            "Kluang, Johor" -> 3340f
            "Kubang Pasu, Kedah" -> 47f
            "Pasir Puteh, Kelantan" -> 21f
            "Alor Gajah, Melaka" -> 36f
            "Kuala Pilah, Negeri Sembilan" -> 44f
            "Rompin, Pahang" -> 840f
            "Seberang Perai Selatan, Pulau Pinang" -> 70f
            "Perak Tengah, Perak" -> 86f
            "Perlis, Perlis" -> 57f
            "Kuala Langat, Selangor" -> 126f
            "Setiu, Terengganu" -> 36f
            "Tuaran, Sabah" -> 207f
            "Samarahan, Sarawak" -> 190f
            else -> 1000f
        }
    }

}