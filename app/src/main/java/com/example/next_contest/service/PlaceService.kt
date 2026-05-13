package com.example.next_contest.service

import com.example.next_contest.BuildConfig
import com.example.next_contest.data.pairing.PairingRepository
import com.example.next_contest.data.pairing.PairingUserProfile
import com.example.next_contest.data.place.KakaoLocalRepository
import com.example.next_contest.data.place.UserPlaceRepository
import com.example.next_contest.model.SavedPlace
import kotlin.concurrent.thread

class PlaceService(
    private val localRepository: KakaoLocalRepository = KakaoLocalRepository(),
    private val placeRepository: UserPlaceRepository = UserPlaceRepository(),
    private val pairingRepository: PairingRepository = PairingRepository()
) {

    fun loadHome(
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        placeRepository.loadHome(onSuccess, onFailure)
    }

    fun saveHome(
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        placeRepository.saveHome(place, onSuccess, onFailure)
    }

    fun loadPairedElderlyHome(
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        loadPairedElderlyUid(
            onSuccess = { elderlyUid ->
                placeRepository.loadHomeForUser(elderlyUid, onSuccess, onFailure)
            },
            onFailure = onFailure
        )
    }

    fun savePairedElderlyHome(
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        loadPairedElderlyUid(
            onSuccess = { elderlyUid ->
                placeRepository.saveHomeForUser(elderlyUid, place, onSuccess, onFailure)
            },
            onFailure = onFailure
        )
    }

    fun loadSafeZone(
        onSuccess: (SavedPlace?) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        placeRepository.loadSafeZone(onSuccess, onFailure)
    }

    fun saveSafeZone(
        place: SavedPlace,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        placeRepository.saveSafeZone(place, onSuccess, onFailure)
    }

    fun findPlaceByAddress(
        query: String,
        onSuccess: (SavedPlace) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val apiKey = BuildConfig.KAKAO_REST_API_KEY
        if (apiKey.isBlank()) {
            onFailure("KAKAO_REST_API_KEY를 local.properties에 추가해주세요.")
            return
        }

        thread {
            try {
                val place = localRepository.findPlaceByAddress(query, apiKey)

                if (place == null) {
                    onFailure("주소 또는 장소를 찾지 못했습니다.")
                } else {
                    onSuccess(place)
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "주소 검색에 실패했습니다.")
            }
        }
    }

    fun makePlaceFromPoint(
        latitude: Double,
        longitude: Double,
        onSuccess: (SavedPlace) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val apiKey = BuildConfig.KAKAO_REST_API_KEY
        if (apiKey.isBlank()) {
            onSuccess(
                SavedPlace(
                    latitude = latitude,
                    longitude = longitude,
                    address = "선택한 위치"
                )
            )
            return
        }

        thread {
            try {
                val address = localRepository.findAddressByCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = apiKey
                )

                onSuccess(
                    SavedPlace(
                        latitude = latitude,
                        longitude = longitude,
                        address = address ?: "선택한 위치"
                    )
                )
            } catch (e: Exception) {
                onFailure(e.message ?: "좌표 주소 변환에 실패했습니다.")
            }
        }
    }

    private fun loadPairedElderlyUid(
        onSuccess: (uid: String) -> Unit,
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

                validateGuardianProfile(
                    currentProfile = currentProfile,
                    onSuccess = { pairedUid ->
                        loadAndValidatePairedElderly(
                            currentUid = currentUid,
                            pairedUid = pairedUid,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    },
                    onFailure = onFailure
                )
            },
            onFailure = { message ->
                onFailure("내 회원 정보 조회 실패: $message")
            }
        )
    }

    private fun validateGuardianProfile(
        currentProfile: PairingUserProfile,
        onSuccess: (pairedUid: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        if (normalizeRole(currentProfile.role) != AuthService.ROLE_GUARDIAN) {
            onFailure("보호자 계정만 어르신 집 위치를 변경할 수 있습니다.")
            return
        }

        val pairedUid = currentProfile.pairedUid
        if (pairedUid.isNullOrBlank()) {
            onFailure("연결된 어르신이 없습니다.")
            return
        }

        onSuccess(pairedUid)
    }

    private fun loadAndValidatePairedElderly(
        currentUid: String,
        pairedUid: String,
        onSuccess: (uid: String) -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        pairingRepository.loadUserProfile(
            uid = pairedUid,
            onSuccess = { pairedProfile ->
                if (pairedProfile == null) {
                    onFailure("연결된 어르신 정보를 찾지 못했습니다.")
                    return@loadUserProfile
                }

                if (normalizeRole(pairedProfile.role) != AuthService.ROLE_ELDERLY) {
                    onFailure("연결된 사용자가 어르신 계정이 아닙니다.")
                    return@loadUserProfile
                }

                if (pairedProfile.pairedUid != currentUid) {
                    onFailure("상호 연결 정보가 맞지 않습니다. 다시 연결해주세요.")
                    return@loadUserProfile
                }

                onSuccess(pairedUid)
            },
            onFailure = { message ->
                onFailure("연결된 어르신 조회 실패: $message")
            }
        )
    }

    private fun normalizeRole(role: String): String {
        return role.trim().lowercase()
    }
}
