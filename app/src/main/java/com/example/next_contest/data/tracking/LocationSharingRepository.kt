package com.example.next_contest.data.tracking

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

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

        val updates = hashMapOf<String, Any>(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to ServerValue.TIMESTAMP,
            "isOnline" to true
        )

        db.child("locations")
            .child(uid)
            .updateChildren(updates)
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun markCurrentUserOffline() {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "timestamp" to ServerValue.TIMESTAMP,
            "isOnline" to false
        )

        db.child("locations")
            .child(uid)
            .updateChildren(updates)
    }
}
