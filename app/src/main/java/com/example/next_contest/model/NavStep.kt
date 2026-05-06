package com.example.next_contest.model

data class NavStep(
    val instruction: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Int,
)