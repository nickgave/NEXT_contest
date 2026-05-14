package com.example.next_contest.model

data class PatientLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long?,
    val isOnline: Boolean,
    val sos: Boolean,
    val safeZoneAlert: Boolean = false,
    val safeZoneAlertDistanceMeters: Int? = null,
    val safeZoneRadiusMeters: Int? = null
)
