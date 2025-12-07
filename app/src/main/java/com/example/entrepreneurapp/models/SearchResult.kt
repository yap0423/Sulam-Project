package com.example.entrepreneurapp.models

sealed class SearchResult {
    data class PersonResult(
        val userId: String,
        val name: String,
        val avatar: String,
        val businessName: String,
        val region: String,
        val email: String,
        val phone: String
    ) : SearchResult()

    data class FarmResult(
        val farmId: String,
        val farmName: String,
        val location: String,
        val ownerId: String,
        val totalSize: Double
    ) : SearchResult()

    data class BusinessResult(
        val businessId: String,
        val businessName: String,
        val type: String,
        val location: String,
        val ownerId: String
    ) : SearchResult()

    data class AnnouncementResult(
        val announcement: Announcement
    ) : SearchResult()
}

data class GroupedSearchResults(
    val people: List<SearchResult.PersonResult> = emptyList(),
    val farms: List<SearchResult.FarmResult> = emptyList(),
    val businesses: List<SearchResult.BusinessResult> = emptyList(),
    val announcements: List<SearchResult.AnnouncementResult> = emptyList()
) {
    fun getTotalCount(): Int = people.size + farms.size + businesses.size + announcements.size
    fun isEmpty(): Boolean = getTotalCount() == 0
}