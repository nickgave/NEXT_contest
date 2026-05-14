package com.example.next_contest.data.tracking

import com.example.next_contest.model.SafeZoneAlertState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GuardianSafeZoneAlertRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    private var guardianUid: String? = null
    private var pairingListener: ValueEventListener? = null
    private var patientUid: String? = null
    private var locationListener: ValueEventListener? = null
    private var lastAlertActive: Boolean? = null

    fun startWatching(
        onAlert: (SafeZoneAlertState) -> Unit,
        onCleared: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (pairingListener != null) return

        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("로그인이 필요합니다.")
            return
        }

        guardianUid = uid
        pairingListener = db.child("users")
            .child(uid)
            .child("pairedUid")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newPatientUid = snapshot.getValue(String::class.java)

                        if (newPatientUid.isNullOrBlank()) {
                            stopLocationWatcher(resetAlertState = true)
                            return
                        }

                        if (newPatientUid == patientUid && locationListener != null) {
                            return
                        }

                        stopLocationWatcher(resetAlertState = true)
                        observePatientLocation(
                            uid = newPatientUid,
                            onAlert = onAlert,
                            onCleared = onCleared,
                            onError = onError
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onError(error.message)
                    }
                }
            )
    }

    fun stopWatching(resetState: Boolean = false) {
        val uid = guardianUid
        val listener = pairingListener

        if (uid != null && listener != null) {
            db.child("users")
                .child(uid)
                .child("pairedUid")
                .removeEventListener(listener)
        }

        guardianUid = null
        pairingListener = null
        stopLocationWatcher(resetAlertState = resetState)
    }

    private fun stopLocationWatcher(resetAlertState: Boolean) {
        val uid = patientUid
        val listener = locationListener

        if (uid != null && listener != null) {
            db.child("locations")
                .child(uid)
                .removeEventListener(listener)
        }

        patientUid = null
        locationListener = null

        if (resetAlertState) {
            lastAlertActive = null
        }
    }

    private fun observePatientLocation(
        uid: String,
        onAlert: (SafeZoneAlertState) -> Unit,
        onCleared: () -> Unit,
        onError: (String) -> Unit
    ) {
        patientUid = uid
        locationListener = db.child("locations")
            .child(uid)
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isAlertActive =
                            snapshot.child("safeZoneAlert").getValue(Boolean::class.java) ?: false
                        val wasAlertActive = lastAlertActive
                        lastAlertActive = isAlertActive

                        when {
                            isAlertActive && wasAlertActive != true -> {
                                onAlert(snapshot.toSafeZoneAlertState(uid))
                            }

                            !isAlertActive && wasAlertActive == true -> {
                                onCleared()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onError(error.message)
                    }
                }
            )
    }

    private fun DataSnapshot.toSafeZoneAlertState(patientUid: String): SafeZoneAlertState {
        return SafeZoneAlertState(
            patientUid = patientUid,
            distanceMeters = child("safeZoneAlertDistanceMeters").asInt(),
            radiusMeters = child("safeZoneRadiusMeters").asInt()
        )
    }

    private fun DataSnapshot.asInt(): Int? {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
    }
}
