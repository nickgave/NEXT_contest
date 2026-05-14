package com.example.next_contest.data.tracking

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class LocationSharingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun ensureCurrentUserLocationNode(
        onFailure: (message: String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        val sosRef = db.child("locations")
            .child(uid)
            .child("sos")

        sosRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    sosRef.setValue(false)
                        .addOnFailureListener { error ->
                            onFailure(error.message ?: "알 수 없는 오류")
                        }
                }
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun updateCurrentUserLocation(
        latitude: Double,
        longitude: Double,
        onFailure: (message: String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        val baseUpdates = hashMapOf<String, Any?>(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to ServerValue.TIMESTAMP,
            "isOnline" to true
        )

        db.child("users")
            .child(uid)
            .child("safeZone")
            .get()
            .addOnSuccessListener { snapshot ->
                val updates = buildLocationUpdates(
                    baseUpdates = baseUpdates,
                    latitude = latitude,
                    longitude = longitude,
                    safeZone = snapshot.toSafeZone()
                )

                updateLocationNode(uid, updates, onFailure)
            }
            .addOnFailureListener { error ->
                updateLocationNode(uid, baseUpdates, onFailure)
                onFailure(error.message ?: "안전구역을 확인하지 못했습니다.")
            }
    }

    fun markCurrentUserOffline() {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any?>(
            "timestamp" to ServerValue.TIMESTAMP,
            "isOnline" to false
        )

        db.child("locations")
            .child(uid)
            .updateChildren(updates)
    }

    private fun buildLocationUpdates(
        baseUpdates: HashMap<String, Any?>,
        latitude: Double,
        longitude: Double,
        safeZone: SafeZone?
    ): HashMap<String, Any?> {
        val updates = HashMap(baseUpdates)

        if (safeZone == null) {
            clearSafeZoneAlert(updates)
            return updates
        }

        val distanceMeters = distanceBetween(
            latitude1 = latitude,
            longitude1 = longitude,
            latitude2 = safeZone.latitude,
            longitude2 = safeZone.longitude
        )
        val isOutsideSafeZone = distanceMeters > safeZone.radiusMeters

        updates["safeZoneAlert"] = isOutsideSafeZone
        updates["safeZoneAlertDistanceMeters"] = distanceMeters.roundToInt()
        updates["safeZoneRadiusMeters"] = safeZone.radiusMeters
        updates["safeZoneCenterLatitude"] = safeZone.latitude
        updates["safeZoneCenterLongitude"] = safeZone.longitude

        if (isOutsideSafeZone) {
            updates["safeZoneAlertAt"] = ServerValue.TIMESTAMP
        } else {
            updates["safeZoneAlertAt"] = null
            updates["safeZoneAlertClearedAt"] = ServerValue.TIMESTAMP
        }

        return updates
    }

    private fun clearSafeZoneAlert(updates: HashMap<String, Any?>) {
        updates["safeZoneAlert"] = false
        updates["safeZoneAlertAt"] = null
        updates["safeZoneAlertDistanceMeters"] = null
        updates["safeZoneRadiusMeters"] = null
        updates["safeZoneCenterLatitude"] = null
        updates["safeZoneCenterLongitude"] = null
    }

    private fun updateLocationNode(
        uid: String,
        updates: Map<String, Any?>,
        onFailure: (message: String) -> Unit
    ) {
        db.child("locations")
            .child(uid)
            .updateChildren(updates)
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    private fun DataSnapshot.toSafeZone(): SafeZone? {
        if (!exists()) return null

        val latitude = child("latitude").getValue(Double::class.java) ?: return null
        val longitude = child("longitude").getValue(Double::class.java) ?: return null
        val radiusMeters = child("radiusMeters").asInt() ?: return null

        if (radiusMeters <= 0) return null

        return SafeZone(
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )
    }

    private fun DataSnapshot.asInt(): Int? {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
    }

    private fun distanceBetween(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val dLat = Math.toRadians(latitude2 - latitude1)
        val dLng = Math.toRadians(longitude2 - longitude1)
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    private data class SafeZone(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Int
    )

    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
    }
}
