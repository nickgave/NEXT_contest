package com.example.next_contest.model

data class SafeZoneAlertState(
    val patientUid: String,
    val distanceMeters: Int?,
    val radiusMeters: Int?
)
