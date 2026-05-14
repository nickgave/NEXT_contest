package com.example.next_contest.service

import android.app.Activity
import com.example.next_contest.data.auth.AuthRepository
import com.example.next_contest.data.auth.AuthUserProfile
import com.google.firebase.auth.PhoneAuthCredential

data class SignUpRequest(
    val role: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val phoneCredential: PhoneAuthCredential?,
    val verifiedNormalizedPhone: String?
)

data class PhoneVerificationSession(
    val verificationId: String,
    val normalizedPhone: String
)

data class ConfirmedPhoneVerification(
    val credential: PhoneAuthCredential,
    val normalizedPhone: String
)

class AuthService(
    private val authRepository: AuthRepository = AuthRepository()
) {

    fun restoreSession(
        onSuccess: (role: String) -> Unit,
        onNoSession: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val uid = authRepository.getCurrentUserUid()

        if (uid == null) {
            onNoSession()
            return
        }

        authRepository.loadUserRole(
            uid = uid,
            onSuccess = onSuccess,
            onFailure = { message ->
                onFailure("저장된 로그인 정보 확인 실패: $message")
            }
        )
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        val cleanedEmail = email.trim()
        val validationMessage = validateLoginInput(cleanedEmail, password)

        if (validationMessage != null) {
            onFinished()
            onFailure(validationMessage)
            return
        }

        authRepository.signIn(
            email = cleanedEmail,
            password = password,
            onSuccess = { uid ->
                loadRoleAndEnter(uid, onSuccess, onFailure, onFinished)
            },
            onFailure = { message ->
                onFinished()
                onFailure("로그인 실패: $message")
            }
        )
    }

    fun signUp(
        request: SignUpRequest,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        val normalizedPhone = normalizePhoneNumber(request.phoneNumber)
        val cleanedRequest = request.copy(
            name = request.name.trim(),
            phoneNumber = request.phoneNumber.trim(),
            email = request.email.trim()
        )
        val validationMessage = validateSignUpInput(cleanedRequest, normalizedPhone)

        if (validationMessage != null) {
            onFinished()
            onFailure(validationMessage)
            return
        }

        authRepository.createUser(
            email = cleanedRequest.email,
            password = cleanedRequest.password,
            onSuccess = { uid ->
                linkPhoneAndSaveProfile(
                    uid = uid,
                    request = cleanedRequest,
                    normalizedPhone = normalizedPhone,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                    onFinished = onFinished
                )
            },
            onFailure = { message ->
                onFinished()
                onFailure("회원가입 실패: $message")
            }
        )
    }

    fun sendPhoneVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (PhoneVerificationSession) -> Unit,
        onAutoVerified: (ConfirmedPhoneVerification) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val validationMessage = validatePhoneNumber(normalizedPhone)

        if (validationMessage != null) {
            onFailure(validationMessage)
            return
        }

        authRepository.isPhoneRegistered(
            normalizedPhone = normalizedPhone,
            onSuccess = { exists ->
                if (exists) {
                    onFailure("이미 등록된 전화번호입니다.")
                    return@isPhoneRegistered
                }

                authRepository.sendPhoneVerificationCode(
                    activity = activity,
                    phoneNumber = formatPhoneNumberForFirebase(phoneNumber, normalizedPhone),
                    onCodeSent = { verificationId ->
                        onCodeSent(
                            PhoneVerificationSession(
                                verificationId = verificationId,
                                normalizedPhone = normalizedPhone
                            )
                        )
                    },
                    onAutoVerified = { credential ->
                        onAutoVerified(
                            ConfirmedPhoneVerification(
                                credential = credential,
                                normalizedPhone = normalizedPhone
                            )
                        )
                    },
                    onFailure = onFailure
                )
            },
            onFailure = { message ->
                onFailure("전화번호 확인 실패: $message")
            }
        )
    }

    fun confirmPhoneVerificationCode(
        session: PhoneVerificationSession,
        code: String,
        onSuccess: (ConfirmedPhoneVerification) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val cleanedCode = code.trim()

        if (cleanedCode.isBlank()) {
            onFailure("인증번호를 입력해주세요.")
            return
        }

        onSuccess(
            ConfirmedPhoneVerification(
                credential = authRepository.buildPhoneCredential(
                    verificationId = session.verificationId,
                    code = cleanedCode
                ),
                normalizedPhone = session.normalizedPhone
            )
        )
    }

    private fun loadRoleAndEnter(
        uid: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        authRepository.loadUserRole(
            uid = uid,
            onSuccess = { role ->
                onFinished()
                onSuccess(role)
            },
            onFailure = { message ->
                authRepository.signOut()
                onFinished()
                onFailure("회원 정보 불러오기 실패: $message")
            }
        )
    }

    private fun linkPhoneAndSaveProfile(
        uid: String,
        request: SignUpRequest,
        normalizedPhone: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        val credential = request.phoneCredential

        if (credential == null) {
            authRepository.deleteCurrentUserThenSignOut()
            onFinished()
            onFailure("휴대폰 인증을 완료해주세요.")
            return
        }

        authRepository.linkPhoneCredential(
            credential = credential,
            onSuccess = {
                checkPhoneAndSaveProfile(
                    uid = uid,
                    request = request,
                    normalizedPhone = normalizedPhone,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                    onFinished = onFinished
                )
            },
            onFailure = { message ->
                authRepository.deleteCurrentUserThenSignOut()
                onFinished()
                onFailure("휴대폰 인증 실패: $message")
            }
        )
    }

    private fun checkPhoneAndSaveProfile(
        uid: String,
        request: SignUpRequest,
        normalizedPhone: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        authRepository.isPhoneRegistered(
            normalizedPhone = normalizedPhone,
            onSuccess = { exists ->
                if (exists) {
                    authRepository.deleteCurrentUserThenSignOut()
                    onFinished()
                    onFailure("이미 등록된 전화번호입니다.")
                    return@isPhoneRegistered
                }

                saveProfile(
                    uid = uid,
                    request = request,
                    normalizedPhone = normalizedPhone,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                    onFinished = onFinished
                )
            },
            onFailure = { message ->
                authRepository.deleteCurrentUserThenSignOut()
                onFinished()
                onFailure("전화번호 확인 실패: $message")
            }
        )
    }

    private fun saveProfile(
        uid: String,
        request: SignUpRequest,
        normalizedPhone: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (message: String) -> Unit,
        onFinished: () -> Unit
    ) {
        val profile = AuthUserProfile(
            uid = uid,
            email = request.email,
            name = request.name,
            phoneNumber = request.phoneNumber,
            normalizedPhone = normalizedPhone,
            role = request.role
        )

        authRepository.saveUserProfile(
            profile = profile,
            onSuccess = {
                onFinished()
                onSuccess(request.role)
            },
            onFailure = { message ->
                authRepository.deleteCurrentUserThenSignOut()
                onFinished()
                onFailure("회원 정보 저장 실패: $message")
            }
        )
    }

    private fun validateLoginInput(
        email: String,
        password: String
    ): String? {
        return when {
            email.isBlank() -> "이메일을 입력해주세요."
            password.isBlank() -> "비밀번호를 입력해주세요."
            else -> null
        }
    }

    private fun validateSignUpInput(
        request: SignUpRequest,
        normalizedPhone: String
    ): String? {
        return when {
            request.name.isBlank() -> "이름을 입력해주세요."
            validatePhoneNumber(normalizedPhone) != null -> validatePhoneNumber(normalizedPhone)
            request.phoneCredential == null -> "휴대폰 인증을 완료해주세요."
            request.verifiedNormalizedPhone != normalizedPhone -> "인증한 전화번호와 입력한 전화번호가 다릅니다."
            request.email.isBlank() -> "이메일을 입력해주세요."
            request.password.length < MIN_PASSWORD_LENGTH -> "비밀번호는 6자 이상 입력해주세요."
            else -> null
        }
    }

    private fun validatePhoneNumber(normalizedPhone: String): String? {
        return if (normalizedPhone.length < MIN_PHONE_DIGITS) {
            "전화번호를 정확히 입력해주세요."
        } else {
            null
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }

    private fun formatPhoneNumberForFirebase(
        phoneNumber: String,
        normalizedPhone: String
    ): String {
        val compact = phoneNumber.trim().filter { it.isDigit() || it == '+' }

        return when {
            compact.startsWith("+") -> compact
            normalizedPhone.startsWith("82") -> "+$normalizedPhone"
            normalizedPhone.startsWith("0") -> "+82${normalizedPhone.drop(1)}"
            else -> "+82$normalizedPhone"
        }
    }

    companion object {
        const val ROLE_ELDERLY = "elderly"
        const val ROLE_GUARDIAN = "guardian"
        private const val MIN_PASSWORD_LENGTH = 6
        private const val MIN_PHONE_DIGITS = 9
    }
}
