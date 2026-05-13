package com.example.next_contest.data.pairing

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

data class PairingUserProfile(
    val uid: String,
    val name: String,
    val phoneNumber: String,
    val role: String,
    val pairedUid: String?
)

data class PairingRequestInfo(
    val requestId: String,
    val fromUid: String,
    val toUid: String,
    val fromName: String,
    val toName: String,
    val fromPhone: String,
    val toPhone: String,
    val fromRole: String,
    val toRole: String,
    val status: String
)

class PairingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    fun findUidByPhone(
        normalizedPhone: String,
        onSuccess: (uid: String?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("phoneIndex")
            .child(normalizedPhone)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(String::class.java))
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun loadUserProfile(
        uid: String,
        onSuccess: (profile: PairingUserProfile?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("users")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.toPairingUserProfile(uid))
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun loadRequest(
        requestId: String,
        onSuccess: (request: PairingRequestInfo?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("pairingRequests")
            .child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.toPairingRequestInfo(requestId))
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun loadFirstRequestForUser(
        uid: String,
        onSuccess: (request: PairingRequestInfo?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("pairingRequestsByUser")
            .child(uid)
            .get()
            .addOnSuccessListener { indexSnapshot ->
                val requestId = indexSnapshot.children
                    .mapNotNull { it.key }
                    .firstOrNull()

                if (requestId == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }

                loadRequest(requestId, onSuccess, onFailure)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun savePairingRequest(
        request: PairingRequestInfo,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val updates = hashMapOf<String, Any?>(
            "pairingRequests/${request.requestId}/requestId" to request.requestId,
            "pairingRequests/${request.requestId}/fromUid" to request.fromUid,
            "pairingRequests/${request.requestId}/toUid" to request.toUid,
            "pairingRequests/${request.requestId}/fromName" to request.fromName,
            "pairingRequests/${request.requestId}/toName" to request.toName,
            "pairingRequests/${request.requestId}/fromPhone" to request.fromPhone,
            "pairingRequests/${request.requestId}/toPhone" to request.toPhone,
            "pairingRequests/${request.requestId}/fromRole" to request.fromRole,
            "pairingRequests/${request.requestId}/toRole" to request.toRole,
            "pairingRequests/${request.requestId}/status" to STATUS_PENDING,
            "pairingRequests/${request.requestId}/createdAt" to ServerValue.TIMESTAMP,
            "pairingRequestsByUser/${request.fromUid}/${request.requestId}" to true,
            "pairingRequestsByUser/${request.toUid}/${request.requestId}" to true
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun cancelRequest(
        request: PairingRequestInfo,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        removeRequest(request, onSuccess, onFailure)
    }

    fun rejectRequest(
        request: PairingRequestInfo,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        removeRequest(request, onSuccess, onFailure)
    }

    fun disconnectPairing(
        currentUid: String,
        pairedUid: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val updates = hashMapOf<String, Any?>(
            "users/$currentUid/pairedUid" to null,
            "users/$currentUid/pairedAt" to null,
            "users/$pairedUid/pairedUid" to null,
            "users/$pairedUid/pairedAt" to null
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun acceptRequest(
        request: PairingRequestInfo,
        guardianUid: String,
        elderlyUid: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val updates = hashMapOf<String, Any?>(
            "users/$guardianUid/pairedUid" to elderlyUid,
            "users/$guardianUid/pairedAt" to ServerValue.TIMESTAMP,
            "users/$elderlyUid/pairedUid" to guardianUid,
            "users/$elderlyUid/pairedAt" to ServerValue.TIMESTAMP,
            "pairingRequests/${request.requestId}" to null,
            "pairingRequestsByUser/${request.fromUid}/${request.requestId}" to null,
            "pairingRequestsByUser/${request.toUid}/${request.requestId}" to null
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    private fun removeRequest(
        request: PairingRequestInfo,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val updates = hashMapOf<String, Any?>(
            "pairingRequests/${request.requestId}" to null,
            "pairingRequestsByUser/${request.fromUid}/${request.requestId}" to null,
            "pairingRequestsByUser/${request.toUid}/${request.requestId}" to null
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    private fun DataSnapshot.toPairingUserProfile(uid: String): PairingUserProfile? {
        if (!exists()) return null

        return PairingUserProfile(
            uid = uid,
            name = child("name").getValue(String::class.java) ?: "사용자",
            phoneNumber = child("phoneNumber").getValue(String::class.java) ?: "",
            role = child("role").getValue(String::class.java) ?: "",
            pairedUid = child("pairedUid").getValue(String::class.java)
        )
    }

    private fun DataSnapshot.toPairingRequestInfo(requestId: String): PairingRequestInfo? {
        if (!exists()) return null

        val status = child("status").getValue(String::class.java) ?: return null
        if (status != STATUS_PENDING) return null

        return PairingRequestInfo(
            requestId = requestId,
            fromUid = child("fromUid").getValue(String::class.java) ?: return null,
            toUid = child("toUid").getValue(String::class.java) ?: return null,
            fromName = child("fromName").getValue(String::class.java) ?: "사용자",
            toName = child("toName").getValue(String::class.java) ?: "사용자",
            fromPhone = child("fromPhone").getValue(String::class.java) ?: "",
            toPhone = child("toPhone").getValue(String::class.java) ?: "",
            fromRole = child("fromRole").getValue(String::class.java) ?: "",
            toRole = child("toRole").getValue(String::class.java) ?: "",
            status = status
        )
    }

    companion object {
        const val STATUS_PENDING = "pending"
    }
}
