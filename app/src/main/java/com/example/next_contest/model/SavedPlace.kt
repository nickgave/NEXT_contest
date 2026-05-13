package com.example.next_contest.model

data class SavedPlace(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusMeters: Int? = null
)
