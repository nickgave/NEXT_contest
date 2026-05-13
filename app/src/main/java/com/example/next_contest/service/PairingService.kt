package com.example.next_contest.service

import com.example.next_contest.data.pairing.PairingRepository
import com.example.next_contest.data.pairing.PairingRequestInfo
import com.example.next_contest.data.pairing.PairingUserProfile

enum class PairingStatus {
    NONE,
    CONNECTED,
    OUTGOING_REQUEST,
    INCOMING_REQUEST
}

data class PairingViewState(
    val status: PairingStatus,
    val requestId: String? = null,
    val counterpartName: String = "",
    val counterpartPhone: String = "",
    val counterpartRole: String = ""
)

class PairingService(
    private val pairingRepository: PairingRepository = PairingRepository()
) {

    fun loadPairingState(
        onSuccess: (PairingViewState) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val currentUid = pairingRepository.getCurrentUid()

        if (currentUid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        pairingRepository.loadUserProfile(
            uid = currentUid,
            onSuccess = { currentProfile ->
                if (currentProfile == null) {
                    onFailure("내 회원 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                currentProfile.pairedUid?.let { pairedUid ->
                    loadConnectedState(pairedUid, onSuccess, onFailure)
                    return@loadUserProfile
                }

                loadPendingState(currentUid, onSuccess, onFailure)
            },
            onFailure = { message ->
                onFailure("내 회원 정보 조회 실패: $message")
            }
        )
    }

    fun requestPairing(
        phoneNumber: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val currentUid = pairingRepository.getCurrentUid()

        if (currentUid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        val normalizedPhone = normalizePhoneNumber(phoneNumber)

        if (normalizedPhone.length < MIN_PHONE_DIGITS) {
            onFailure("전화번호를 정확히 입력해주세요.")
            return
        }

        pairingRepository.findUidByPhone(
            normalizedPhone = normalizedPhone,
            onSuccess = { targetUid ->
                if (targetUid == null) {
                    onFailure("해당 전화번호로 가입된 사용자를 찾지 못했습니다.")
                    return@findUidByPhone
                }

                if (targetUid == currentUid) {
                    onFailure("본인 전화번호와는 연결할 수 없습니다.")
                    return@findUidByPhone
                }

                loadUsersAndRequest(
                    currentUid = currentUid,
                    targetUid = targetUid,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            },
            onFailure = { message ->
                onFailure("전화번호 조회 실패: $message")
            }
        )
    }

    fun acceptRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val currentUid = pairingRepository.getCurrentUid()

        if (currentUid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        pairingRepository.loadRequest(
            requestId = requestId,
            onSuccess = { request ->
                if (request == null) {
                    onFailure("연결 요청을 찾지 못했습니다.")
                    return@loadRequest
                }

                if (request.toUid != currentUid) {
                    onFailure("내게 온 연결 요청만 수락할 수 있습니다.")
                    return@loadRequest
                }

                acceptLoadedRequest(request, onSuccess, onFailure)
            },
            onFailure = { message ->
                onFailure("연결 요청 조회 실패: $message")
            }
        )
    }

    fun cancelRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val currentUid = pairingRepository.getCurrentUid()

        if (currentUid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        pairingRepository.loadRequest(
            requestId = requestId,
            onSuccess = { request ->
                if (request == null) {
                    onFailure("연결 요청을 찾지 못했습니다.")
                    return@loadRequest
                }

                if (request.fromUid != currentUid && request.toUid != currentUid) {
                    onFailure("내 연결 요청만 취소할 수 있습니다.")
                    return@loadRequest
                }

                pairingRepository.cancelRequest(
                    request = request,
                    onSuccess = onSuccess,
                    onFailure = { message ->
                        onFailure("연결 요청 취소 실패: $message")
                    }
                )
            },
            onFailure = { message ->
                onFailure("연결 요청 조회 실패: $message")
            }
        )
    }

    fun disconnectPairing(
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val currentUid = pairingRepository.getCurrentUid()

        if (currentUid == null) {
            onFailure("로그인이 필요합니다.")
            return
        }

        pairingRepository.loadUserProfile(
            uid = currentUid,
            onSuccess = { currentProfile ->
                if (currentProfile == null) {
                    onFailure("내 회원 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                val pairedUid = currentProfile.pairedUid
                if (pairedUid.isNullOrBlank()) {
                    onFailure("연결된 사용자가 없습니다.")
                    return@loadUserProfile
                }

                pairingRepository.disconnectPairing(
                    currentUid = currentUid,
                    pairedUid = pairedUid,
                    onSuccess = onSuccess,
                    onFailure = { message ->
                        onFailure("연결 해제 실패: $message")
                    }
                )
            },
            onFailure = { message ->
                onFailure("내 회원 정보 조회 실패: $message")
            }
        )
    }

    private fun loadConnectedState(
        pairedUid: String,
        onSuccess: (PairingViewState) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        pairingRepository.loadUserProfile(
            uid = pairedUid,
            onSuccess = { pairedProfile ->
                if (pairedProfile == null) {
                    onFailure("연결된 사용자 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                onSuccess(
                    PairingViewState(
                        status = PairingStatus.CONNECTED,
                        counterpartName = pairedProfile.name,
                        counterpartPhone = pairedProfile.phoneNumber,
                        counterpartRole = pairedProfile.role
                    )
                )
            },
            onFailure = { message ->
                onFailure("연결 사용자 조회 실패: $message")
            }
        )
    }

    private fun loadPendingState(
        currentUid: String,
        onSuccess: (PairingViewState) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        pairingRepository.loadFirstRequestForUser(
            uid = currentUid,
            onSuccess = { request ->
                if (request == null) {
                    onSuccess(PairingViewState(PairingStatus.NONE))
                    return@loadFirstRequestForUser
                }

                onSuccess(request.toViewState(currentUid))
            },
            onFailure = { message ->
                onFailure("연결 요청 조회 실패: $message")
            }
        )
    }

    private fun loadUsersAndRequest(
        currentUid: String,
        targetUid: String,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        pairingRepository.loadUserProfile(
            uid = currentUid,
            onSuccess = { currentProfile ->
                if (currentProfile == null) {
                    onFailure("내 회원 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                if (currentProfile.pairedUid != null) {
                    onFailure("이미 연결된 사용자가 있습니다.")
                    return@loadUserProfile
                }

                pairingRepository.loadUserProfile(
                    uid = targetUid,
                    onSuccess = { targetProfile ->
                        validateAndSaveRequest(
                            currentProfile = currentProfile,
                            targetProfile = targetProfile,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    },
                    onFailure = { message ->
                        onFailure("상대방 회원 정보 조회 실패: $message")
                    }
                )
            },
            onFailure = { message ->
                onFailure("내 회원 정보 조회 실패: $message")
            }
        )
    }

    private fun validateAndSaveRequest(
        currentProfile: PairingUserProfile,
        targetProfile: PairingUserProfile?,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        if (targetProfile == null) {
            onFailure("상대방 회원 정보를 찾지 못했습니다.")
            return
        }

        if (targetProfile.pairedUid != null) {
            onFailure("상대방은 이미 연결된 사용자가 있습니다.")
            return
        }

        val currentRole = normalizeRole(currentProfile.role)
        val targetRole = normalizeRole(targetProfile.role)

        if (!isGuardianElderlyPair(currentRole, targetRole)) {
            onFailure("보호자와 어르신 계정끼리만 연결할 수 있습니다.")
            return
        }

        val requestId = makeRequestId(currentProfile.uid, targetProfile.uid)

        pairingRepository.loadRequest(
            requestId = requestId,
            onSuccess = { existingRequest ->
                if (existingRequest != null) {
                    val message = if (existingRequest.fromUid == currentProfile.uid) {
                        "이미 연결 요청을 보냈습니다."
                    } else {
                        "상대방이 이미 연결 요청을 보냈습니다. 요청을 수락해주세요."
                    }

                    onFailure(message)
                    return@loadRequest
                }

                saveRequest(
                    requestId = requestId,
                    currentProfile = currentProfile.copy(role = currentRole),
                    targetProfile = targetProfile.copy(role = targetRole),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            },
            onFailure = { message ->
                onFailure("기존 요청 확인 실패: $message")
            }
        )
    }

    private fun saveRequest(
        requestId: String,
        currentProfile: PairingUserProfile,
        targetProfile: PairingUserProfile,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val request = PairingRequestInfo(
            requestId = requestId,
            fromUid = currentProfile.uid,
            toUid = targetProfile.uid,
            fromName = currentProfile.name,
            toName = targetProfile.name,
            fromPhone = currentProfile.phoneNumber,
            toPhone = targetProfile.phoneNumber,
            fromRole = currentProfile.role,
            toRole = targetProfile.role,
            status = PairingRepository.STATUS_PENDING
        )

        pairingRepository.savePairingRequest(
            request = request,
            onSuccess = onSuccess,
            onFailure = { message ->
                onFailure("연결 요청 저장 실패: $message")
            }
        )
    }

    private fun acceptLoadedRequest(
        request: PairingRequestInfo,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val fromRole = normalizeRole(request.fromRole)
        val toRole = normalizeRole(request.toRole)

        val guardianUid: String
        val elderlyUid: String

        when {
            fromRole == AuthService.ROLE_GUARDIAN && toRole == AuthService.ROLE_ELDERLY -> {
                guardianUid = request.fromUid
                elderlyUid = request.toUid
            }

            fromRole == AuthService.ROLE_ELDERLY && toRole == AuthService.ROLE_GUARDIAN -> {
                guardianUid = request.toUid
                elderlyUid = request.fromUid
            }

            else -> {
                onFailure("보호자와 어르신 계정끼리만 연결할 수 있습니다.")
                return
            }
        }

        pairingRepository.acceptRequest(
            request = request,
            guardianUid = guardianUid,
            elderlyUid = elderlyUid,
            onSuccess = onSuccess,
            onFailure = { message ->
                onFailure("연결 수락 실패: $message")
            }
        )
    }

    private fun PairingRequestInfo.toViewState(currentUid: String): PairingViewState {
        return if (fromUid == currentUid) {
            PairingViewState(
                status = PairingStatus.OUTGOING_REQUEST,
                requestId = requestId,
                counterpartName = toName,
                counterpartPhone = toPhone,
                counterpartRole = toRole
            )
        } else {
            PairingViewState(
                status = PairingStatus.INCOMING_REQUEST,
                requestId = requestId,
                counterpartName = fromName,
                counterpartPhone = fromPhone,
                counterpartRole = fromRole
            )
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }

    private fun normalizeRole(role: String): String {
        return role.trim().lowercase()
    }

    private fun makeRequestId(uidA: String, uidB: String): String {
        return listOf(uidA, uidB).sorted().joinToString("_")
    }

    private fun isGuardianElderlyPair(roleA: String, roleB: String): Boolean {
        return setOf(roleA, roleB) == setOf(
            AuthService.ROLE_GUARDIAN,
            AuthService.ROLE_ELDERLY
        )
    }

    companion object {
        private const val MIN_PHONE_DIGITS = 9
    }
}
