package com.example.next_contest.data.tracking

import com.example.next_contest.model.PatientLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class TrackingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    private var locationListener: ValueEventListener? = null
    private var trackedUid: String? = null

    fun startWatchingPatientLocation(
        onNoPatient: (String) -> Unit,
        onPatientNameLoaded: (String) -> Unit,
        onLocationChanged: (PatientLocation) -> Unit,
        onLocationMissing: () -> Unit,
        onError: (String) -> Unit
    ) {
        val guardianUid = auth.currentUser?.uid

        if (guardianUid == null) {
            onNoPatient("로그인이 필요합니다.")
            return
        }

        db.child("users")
            .child(guardianUid)
            .child("pairedUid")
            .get()
            .addOnSuccessListener { snap ->
                val patientUid = snap.getValue(String::class.java)

                if (patientUid == null) {
                    onNoPatient("연결된 어르신이 없습니다.\n설정에서 먼저 연결해주세요.")
                    return@addOnSuccessListener
                }

                trackedUid = patientUid
                loadPatientName(patientUid, onPatientNameLoaded)
                observePatientLocation(
                    patientUid = patientUid,
                    onLocationChanged = onLocationChanged,
                    onLocationMissing = onLocationMissing,
                    onError = onError
                )
            }
            .addOnFailureListener {
                onNoPatient("연결 정보를 불러오지 못했습니다.")
            }
    }

    fun dismissSos(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = trackedUid

        if (uid == null) {
            onFailure("추적 중인 어르신 정보가 없습니다.")
            return
        }

        val updates = hashMapOf<String, Any?>(
            "sos" to false,
            "safeZoneAlert" to false,
            "safeZoneAlertAt" to null,
            "safeZoneAlertClearedAt" to ServerValue.TIMESTAMP
        )

        db.child("locations")
            .child(uid)
            .updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "알 수 없는 오류")
            }
    }

    fun stopWatchingPatientLocation() {
        val uid = trackedUid
        val listener = locationListener

        if (uid != null && listener != null) {
            db.child("locations")
                .child(uid)
                .removeEventListener(listener)
        }

        trackedUid = null
        locationListener = null
    }

    private fun loadPatientName(
        patientUid: String,
        onPatientNameLoaded: (String) -> Unit
    ) {
        db.child("users")
            .child(patientUid)
            .child("name")
            .get()
            .addOnSuccessListener { nameSnap ->
                val name = nameSnap.getValue(String::class.java) ?: "어르신"
                onPatientNameLoaded(name)
            }
    }

    private fun observePatientLocation(
        patientUid: String,
        onLocationChanged: (PatientLocation) -> Unit,
        onLocationMissing: () -> Unit,
        onError: (String) -> Unit
    ) {
        locationListener = db.child("locations")
            .child(patientUid)
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val location = snapshotToPatientLocation(snapshot)

                        if (location == null) {
                            onLocationMissing()
                            return
                        }

                        onLocationChanged(location)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onError(error.message)
                    }
                }
            )
    }

    private fun snapshotToPatientLocation(snapshot: DataSnapshot): PatientLocation? {
        val lat = snapshot.child("latitude").getValue(Double::class.java)
        val lng = snapshot.child("longitude").getValue(Double::class.java)
        val ts = snapshot.child("timestamp").getValue(Long::class.java)
        val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
        val sos = snapshot.child("sos").getValue(Boolean::class.java) ?: false
        val safeZoneAlert = snapshot.child("safeZoneAlert").getValue(Boolean::class.java) ?: false
        val safeZoneAlertDistanceMeters = snapshot.child("safeZoneAlertDistanceMeters").asInt()
        val safeZoneRadiusMeters = snapshot.child("safeZoneRadiusMeters").asInt()

        if (lat == null || lng == null) {
            return null
        }

        return PatientLocation(
            latitude = lat,
            longitude = lng,
            timestamp = ts,
            isOnline = isOnline,
            sos = sos,
            safeZoneAlert = safeZoneAlert,
            safeZoneAlertDistanceMeters = safeZoneAlertDistanceMeters,
            safeZoneRadiusMeters = safeZoneRadiusMeters
        )
    }

    private fun DataSnapshot.asInt(): Int? {
        return getValue(Int::class.java)
            ?: getValue(Long::class.java)?.toInt()
            ?: getValue(Double::class.java)?.toInt()
    }
}
