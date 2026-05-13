package com.example.next_contest.service

import com.example.next_contest.BuildConfig
import com.example.next_contest.data.place.KakaoLocalRepository
import com.example.next_contest.data.place.UserPlaceRepository
import com.example.next_contest.model.SavedPlace
import kotlin.concurrent.thread

class PlaceService(
    private val localRepository: KakaoLocalRepository = KakaoLocalRepository(),
    private val placeRepository: UserPlaceRepository = UserPlaceRepository()
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
}
