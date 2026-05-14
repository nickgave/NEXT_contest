package com.example.next_contest.service

import com.example.next_contest.data.missing.MissingReportRepository
import com.example.next_contest.data.pairing.PairingRepository
import com.example.next_contest.data.pairing.PairingUserProfile

class MissingReportService(
    private val pairingRepository: PairingRepository = PairingRepository(),
    private val missingReportRepository: MissingReportRepository = MissingReportRepository()
) {
    fun submitMissingReport(
        onSuccess: (reportId: String) -> Unit,
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
                val guardian = currentProfile

                if (guardian == null) {
                    onFailure("보호자 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                if (guardian.role.trim().lowercase() != AuthService.ROLE_GUARDIAN) {
                    onFailure("실종 신고는 보호자 계정에서만 가능합니다.")
                    return@loadUserProfile
                }

                val elderlyUid = guardian.pairedUid

                if (elderlyUid == null) {
                    onFailure("연결된 어르신이 없습니다. 먼저 설정에서 연결해주세요.")
                    return@loadUserProfile
                }

                loadElderlyAndCreateReport(
                    guardian = guardian,
                    elderlyUid = elderlyUid,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            },
            onFailure = onFailure
        )
    }

    private fun loadElderlyAndCreateReport(
        guardian: PairingUserProfile,
        elderlyUid: String,
        onSuccess: (reportId: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        pairingRepository.loadUserProfile(
            uid = elderlyUid,
            onSuccess = { elderly ->
                if (elderly == null) {
                    onFailure("연결된 어르신 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                if (elderly.role.trim().lowercase() != AuthService.ROLE_ELDERLY) {
                    onFailure("연결된 사용자가 어르신 계정이 아닙니다.")
                    return@loadUserProfile
                }

                if (elderly.pairedUid != guardian.uid) {
                    onFailure("양방향 연결 상태가 맞지 않습니다. 설정에서 다시 연결해주세요.")
                    return@loadUserProfile
                }

                missingReportRepository.loadLastLocation(
                    elderlyUid = elderly.uid,
                    onSuccess = { lastLocation ->
                        missingReportRepository.createMissingReport(
                            guardian = guardian,
                            elderly = elderly,
                            lastLocation = lastLocation,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    },
                    onFailure = onFailure
                )
            },
            onFailure = onFailure
        )
    }
}
