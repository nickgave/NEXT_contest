package com.example.next_contest.data.tracking

import com.example.next_contest.model.PatientLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PairedLocationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    private var listener: ValueEventListener? = null
    private var pairedUid: String? = null

    fun startWatchingPairedLocation(
        onNoPair: (String) -> Unit,
        onPairedNameLoaded: (String) -> Unit,
        onLocationChanged: (PatientLocation) -> Unit,
        onLocationMissing: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUid = auth.currentUser?.uid

        if (currentUid == null) {
            onNoPair("로그인이 필요합니다.")
            return
        }

        db.child("users")
            .child(currentUid)
            .child("pairedUid")
            .get()
            .addOnSuccessListener { snapshot ->
                val uid = snapshot.getValue(String::class.java)

                if (uid == null) {
                    onNoPair("연결된 사용자가 없습니다.\n설정에서 먼저 연결해주세요.")
                    return@addOnSuccessListener
                }

                pairedUid = uid
                loadPairedName(uid, onPairedNameLoaded)
                observeLocation(uid, onLocationChanged, onLocationMissing, onError)
            }
            .addOnFailureListener { error ->
                onNoPair(error.message ?: "연결 정보를 불러오지 못했습니다.")
            }
    }

    fun stopWatchingPairedLocation() {
        val uid = pairedUid
        val activeListener = listener

        if (uid != null && activeListener != null) {
            db.child("locations")
                .child(uid)
                .removeEventListener(activeListener)
        }

        pairedUid = null
        listener = null
    }

    private fun loadPairedName(
        uid: String,
        onPairedNameLoaded: (String) -> Unit
    ) {
        db.child("users")
            .child(uid)
            .child("name")
            .get()
            .addOnSuccessListener { snapshot ->
                onPairedNameLoaded(snapshot.getValue(String::class.java) ?: "상대방")
            }
    }

    private fun observeLocation(
        uid: String,
        onLocationChanged: (PatientLocation) -> Unit,
        onLocationMissing: () -> Unit,
        onError: (String) -> Unit
    ) {
        listener = db.child("locations")
            .child(uid)
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

        if (lat == null || lng == null) {
            return null
        }

        return PatientLocation(
            latitude = lat,
            longitude = lng,
            timestamp = ts,
            isOnline = isOnline,
            sos = sos
        )
    }
}
