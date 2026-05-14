package com.example.next_contest.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.concurrent.TimeUnit

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

    fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onAutoVerified: (credential: PhoneAuthCredential) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onAutoVerified(credential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                onFailure(error.message ?: "휴대폰 인증 요청에 실패했습니다.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(PHONE_VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun buildPhoneCredential(
        verificationId: String,
        code: String
    ): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    fun linkPhoneCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onFailure("회원가입 사용자를 확인하지 못했습니다.")
            return
        }

        user.linkWithCredential(credential)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "휴대폰 인증 확인에 실패했습니다.")
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

    companion object {
        private const val PHONE_VERIFICATION_TIMEOUT_SECONDS = 60L
    }
}
