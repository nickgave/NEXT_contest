package com.example.next_contest.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

data class AuthUserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val phoneNumber: String,
    val normalizedPhone: String,
    val role: String
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference
) {

    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    fun signOut() {
        auth.signOut()
    }

    fun signIn(
        email: String,
        password: String,
        onSuccess: (uid: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onFailure("사용자 정보를 확인하지 못했습니다.")
                    return@addOnSuccessListener
                }

                onSuccess(uid)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun createUser(
        email: String,
        password: String,
        onSuccess: (uid: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onFailure("회원가입 정보를 확인하지 못했습니다.")
                    return@addOnSuccessListener
                }

                onSuccess(uid)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun loadUserRole(
        uid: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { roleSnap ->
                val role = roleSnap.getValue(String::class.java)

                if (role == null) {
                    onFailure("회원 정보를 찾지 못했습니다.")
                    return@addOnSuccessListener
                }

                onSuccess(role)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun isPhoneRegistered(
        normalizedPhone: String,
        onSuccess: (exists: Boolean) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        db.child("phoneIndex")
            .child(normalizedPhone)
            .get()
            .addOnSuccessListener { phoneSnap ->
                onSuccess(phoneSnap.exists())
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun saveUserProfile(
        profile: AuthUserProfile,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "users/${profile.uid}/uid" to profile.uid,
            "users/${profile.uid}/email" to profile.email,
            "users/${profile.uid}/name" to profile.name,
            "users/${profile.uid}/phoneNumber" to profile.phoneNumber,
            "users/${profile.uid}/normalizedPhone" to profile.normalizedPhone,
            "users/${profile.uid}/role" to profile.role,
            "users/${profile.uid}/createdAt" to ServerValue.TIMESTAMP,
            "phoneIndex/${profile.normalizedPhone}" to profile.uid
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "알 수 없는 오류")
            }
    }

    fun deleteCurrentUserThenSignOut() {
        val user = auth.currentUser

        if (user == null) {
            auth.signOut()
            return
        }

        user.delete()
            .addOnCompleteListener {
                auth.signOut()
            }
    }

}
