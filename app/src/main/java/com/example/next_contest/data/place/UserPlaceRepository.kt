package com.example.next_contest.data.place

import com.example.next_contest.model.SavedPlace
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class UserPlaceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun loadHome(
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        loadPlace("homeLocation", onSuccess, onFailure)
    }

    fun loadHomeForUser(
        uid: String,
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        loadPlaceForUser(uid, "homeLocation", onSuccess, onFailure)
    }

    fun saveHome(
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        savePlace("homeLocation", place.copy(radiusMeters = null), onSuccess, onFailure)
    }

    fun saveHomeForUser(
        uid: String,
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        savePlaceForUser(uid, "homeLocation", place.copy(radiusMeters = null), onSuccess, onFailure)
    }

    fun loadSafeZone(
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        loadPlace("safeZone", onSuccess, onFailure)
    }

    fun saveSafeZone(
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        savePlace("safeZone", place, onSuccess, onFailure)
    }

    private fun loadPlace(
        childName: String,
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        loadPlaceForUser(uid, childName, onSuccess, onFailure)
    }

    private fun loadPlaceForUser(
        uid: String,
        childName: String,
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("users")
            .child(uid)
            .child(childName)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.toSavedPlace())
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "장소 정보를 불러오지 못했습니다.")
            }
    }

    private fun savePlace(
        childName: String,
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        savePlaceForUser(uid, childName, place, onSuccess, onFailure)
    }

    private fun savePlaceForUser(
        uid: String,
        childName: String,
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val values = hashMapOf<String, Any?>(
            "latitude" to place.latitude,
            "longitude" to place.longitude,
            "address" to place.address,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        if (place.radiusMeters != null) {
            values["radiusMeters"] = place.radiusMeters
        }

        db.child("users")
            .child(uid)
            .child(childName)
            .setValue(values)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "장소 정보를 저장하지 못했습니다.")
            }
    }

    private fun DataSnapshot.toSavedPlace(): SavedPlace? {
        if (!exists()) return null

        val latitude = child("latitude").getValue(Double::class.java) ?: return null
        val longitude = child("longitude").getValue(Double::class.java) ?: return null
        val address = child("address").getValue(String::class.java) ?: "선택한 위치"
        val radiusMeters = child("radiusMeters").getValue(Int::class.java)

        return SavedPlace(
            latitude = latitude,
            longitude = longitude,
            address = address,
            radiusMeters = radiusMeters
        )
    }
}
