package com.example.next_contest.data.missing

import com.example.next_contest.data.pairing.PairingUserProfile
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

data class MissingReportLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long?,
    val isOnline: Boolean
)

class MissingReportRepository(
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {
    fun loadLastLocation(
        elderlyUid: String,
        onSuccess: (MissingReportLocation?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("locations")
            .child(elderlyUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)

                if (lat == null || lng == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                onSuccess(
                    MissingReportLocation(
                        latitude = lat,
                        longitude = lng,
                        timestamp = snapshot.child("timestamp").getValue(Long::class.java),
                        isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    )
                )
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "마지막 위치를 불러오지 못했습니다.")
            }
    }

    fun createMissingReport(
        guardian: PairingUserProfile,
        elderly: PairingUserProfile,
        lastLocation: MissingReportLocation?,
        onSuccess: (reportId: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val reportId = db.child("missingReports").push().key

        if (reportId == null) {
            onFailure("실종 신고 번호를 만들지 못했습니다.")
            return
        }

        val reportPath = "missingReports/$reportId"
        val updates = hashMapOf<String, Any?>(
            "$reportPath/reportId" to reportId,
            "$reportPath/status" to STATUS_OPEN,
            "$reportPath/guardianUid" to guardian.uid,
            "$reportPath/guardianName" to guardian.name,
            "$reportPath/guardianPhone" to guardian.phoneNumber,
            "$reportPath/elderlyUid" to elderly.uid,
            "$reportPath/elderlyName" to elderly.name,
            "$reportPath/elderlyPhone" to elderly.phoneNumber,
            "$reportPath/createdAt" to ServerValue.TIMESTAMP,
            "missingReportsByUser/${guardian.uid}/$reportId" to true,
            "missingReportsByUser/${elderly.uid}/$reportId" to true,
            "missingReportsByStatus/$STATUS_OPEN/$reportId" to true,
            "users/${elderly.uid}/missingStatus" to STATUS_OPEN,
            "users/${elderly.uid}/activeMissingReportId" to reportId,
            "locations/${elderly.uid}/missingReported" to true,
            "locations/${elderly.uid}/missingReportId" to reportId,
            "locations/${elderly.uid}/sos" to true
        )

        if (lastLocation != null) {
            updates["$reportPath/lastLocation/latitude"] = lastLocation.latitude
            updates["$reportPath/lastLocation/longitude"] = lastLocation.longitude
            updates["$reportPath/lastLocation/timestamp"] = lastLocation.timestamp
            updates["$reportPath/lastLocation/isOnline"] = lastLocation.isOnline
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess(reportId)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "실종 신고 저장에 실패했습니다.")
            }
    }

    companion object {
        const val STATUS_OPEN = "open"
    }
}
