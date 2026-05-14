package com.example.next_contest.controller

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.next_contest.R
import com.example.next_contest.model.SavedPlace
import com.example.next_contest.service.PlaceService
import com.kakao.vectormap.MapView

enum class HomeSettingTarget {
    CURRENT_USER,
    PAIRED_ELDERLY
}

class PlaceSettingController(
    private val activity: AppCompatActivity,
    private val placeService: PlaceService = PlaceService()
) {
    private lateinit var mapController: KakaoPlacePickerMapController
    private lateinit var titleText: TextView
    private lateinit var addressInput: EditText
    private lateinit var selectedAddressText: TextView
    private lateinit var radiusLayout: LinearLayout
    private lateinit var radiusValueText: TextView
    private lateinit var radiusSeekBar: SeekBar
    private lateinit var saveButton: Button
    private var selectedPlace: SavedPlace? = null
    private var radiusMeters: Int = DEFAULT_RADIUS_METERS
    private var currentMode: PlaceSettingMode = PlaceSettingMode.HOME
    private var currentHomeTarget: HomeSettingTarget = HomeSettingTarget.CURRENT_USER

    fun showHomeSetting(
        defaultPlace: SavedPlace,
        target: HomeSettingTarget = HomeSettingTarget.CURRENT_USER,
        onBack: () -> Unit,
        onSaved: (SavedPlace) -> Unit
    ) {
        show(
            mode = PlaceSettingMode.HOME,
            defaultPlace = defaultPlace,
            homeTarget = target,
            onBack = onBack,
            onSaved = onSaved
        )
    }

    fun showSafeZoneSetting(
        defaultPlace: SavedPlace,
        target: HomeSettingTarget = HomeSettingTarget.CURRENT_USER,
        onBack: () -> Unit,
        onSaved: (SavedPlace) -> Unit
    ) {
        show(
            mode = PlaceSettingMode.SAFE_ZONE,
            defaultPlace = defaultPlace,
            homeTarget = target,
            onBack = onBack,
            onSaved = onSaved
        )
    }

    fun stop() {
        if (::mapController.isInitialized) {
            mapController.stop()
        }
    }

    private fun show(
        mode: PlaceSettingMode,
        defaultPlace: SavedPlace,
        homeTarget: HomeSettingTarget,
        onBack: () -> Unit,
        onSaved: (SavedPlace) -> Unit
    ) {
        stop()
        currentMode = mode
        currentHomeTarget = homeTarget
        selectedPlace = null
        radiusMeters = DEFAULT_RADIUS_METERS

        activity.setContentView(R.layout.activity_place_setting)
        bindViews()

        titleText.text = makeTitle(mode, homeTarget)
        saveButton.text = makeSaveButtonText(mode, homeTarget)
        radiusLayout.visibility = if (mode == PlaceSettingMode.SAFE_ZONE) View.VISIBLE else View.GONE

        mapController = KakaoPlacePickerMapController(
            activity.findViewById<MapView>(R.id.placeMapView)
        )
        mapController.start { latitude, longitude ->
            selectPoint(latitude, longitude)
        }

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            stop()
            onBack()
        }

        activity.findViewById<Button>(R.id.btnSearchAddress).setOnClickListener {
            searchAddress()
        }

        radiusSeekBar.progress = DEFAULT_RADIUS_METERS - MIN_RADIUS_METERS
        updateRadiusText()
        radiusSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    radiusMeters = progress + MIN_RADIUS_METERS
                    updateRadiusText()
                    mapController.updateRadius(radiusMeters)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            }
        )

        saveButton.setOnClickListener {
            saveSelectedPlace(onSaved)
        }

        showSelectedPlace(defaultPlace, moveCamera = true)
        loadSavedPlace(mode, defaultPlace)
    }

    private fun bindViews() {
        titleText = activity.findViewById(R.id.tvPlaceTitle)
        addressInput = activity.findViewById(R.id.etPlaceAddress)
        selectedAddressText = activity.findViewById(R.id.tvSelectedAddress)
        radiusLayout = activity.findViewById(R.id.layoutRadius)
        radiusValueText = activity.findViewById(R.id.tvRadiusValue)
        radiusSeekBar = activity.findViewById(R.id.seekBarRadius)
        saveButton = activity.findViewById(R.id.btnSavePlace)
    }

    private fun loadSavedPlace(
        mode: PlaceSettingMode,
        defaultPlace: SavedPlace
    ) {
        val onSuccess: (SavedPlace?) -> Unit = { savedPlace ->
            activity.runOnUiThread {
                val place = savedPlace ?: defaultPlace
                if (mode == PlaceSettingMode.SAFE_ZONE) {
                    radiusMeters = place.radiusMeters ?: DEFAULT_RADIUS_METERS
                    radiusSeekBar.progress = (radiusMeters - MIN_RADIUS_METERS)
                        .coerceIn(0, radiusSeekBar.max)
                    updateRadiusText()
                }
                showSelectedPlace(place, moveCamera = true)
            }
        }
        val onFailure: (String) -> Unit = { message ->
            activity.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                showSelectedPlace(defaultPlace, moveCamera = true)
            }
        }

        if (mode == PlaceSettingMode.HOME) {
            if (currentHomeTarget == HomeSettingTarget.PAIRED_ELDERLY) {
                placeService.loadPairedElderlyHome(onSuccess, onFailure)
            } else {
                placeService.loadHome(onSuccess, onFailure)
            }
        } else {
            if (currentHomeTarget == HomeSettingTarget.PAIRED_ELDERLY) {
                placeService.loadPairedElderlySafeZone(onSuccess, onFailure)
            } else {
                placeService.loadSafeZone(onSuccess, onFailure)
            }
        }
    }

    private fun searchAddress() {
        val query = addressInput.text.toString()
        if (query.isBlank()) {
            Toast.makeText(activity, "주소 또는 장소를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        placeService.findPlaceByAddress(
            query = query,
            onSuccess = { place ->
                activity.runOnUiThread {
                    setLoading(false)
                    showSelectedPlace(place, moveCamera = true)
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    setLoading(false)
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun selectPoint(
        latitude: Double,
        longitude: Double
    ) {
        setLoading(true)
        placeService.makePlaceFromPoint(
            latitude = latitude,
            longitude = longitude,
            onSuccess = { place ->
                activity.runOnUiThread {
                    setLoading(false)
                    showSelectedPlace(place, moveCamera = false)
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    setLoading(false)
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showSelectedPlace(
        place: SavedPlace,
        moveCamera: Boolean
    ) {
        val placeWithRadius = if (currentMode == PlaceSettingMode.SAFE_ZONE) {
            place.copy(radiusMeters = radiusMeters)
        } else {
            place.copy(radiusMeters = null)
        }

        selectedPlace = placeWithRadius
        addressInput.setText(place.address)
        selectedAddressText.text = buildString {
            append(place.address)
            append("\n")
            append("%.5f, %.5f".format(place.latitude, place.longitude))
        }
        mapController.showPlace(
            place = placeWithRadius,
            radiusMeters = placeWithRadius.radiusMeters,
            moveCamera = moveCamera
        )
    }

    private fun saveSelectedPlace(onSaved: (SavedPlace) -> Unit) {
        val place = selectedPlace
        if (place == null) {
            Toast.makeText(activity, "저장할 위치를 먼저 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val finalPlace = if (currentMode == PlaceSettingMode.SAFE_ZONE) {
            place.copy(radiusMeters = radiusMeters)
        } else {
            place.copy(radiusMeters = null)
        }

        setLoading(true)
        val onSuccess = {
            activity.runOnUiThread {
                setLoading(false)
                Toast.makeText(activity, "저장했습니다.", Toast.LENGTH_SHORT).show()
                onSaved(finalPlace)
            }
        }
        val onFailure: (String) -> Unit = { message ->
            activity.runOnUiThread {
                setLoading(false)
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }

        if (currentMode == PlaceSettingMode.HOME) {
            if (currentHomeTarget == HomeSettingTarget.PAIRED_ELDERLY) {
                placeService.savePairedElderlyHome(finalPlace, onSuccess, onFailure)
            } else {
                placeService.saveHome(finalPlace, onSuccess, onFailure)
            }
        } else {
            if (currentHomeTarget == HomeSettingTarget.PAIRED_ELDERLY) {
                placeService.savePairedElderlySafeZone(finalPlace, onSuccess, onFailure)
            } else {
                placeService.saveSafeZone(finalPlace, onSuccess, onFailure)
            }
        }
    }

    private fun makeTitle(
        mode: PlaceSettingMode,
        homeTarget: HomeSettingTarget
    ): String {
        return when {
            mode == PlaceSettingMode.SAFE_ZONE &&
                homeTarget == HomeSettingTarget.PAIRED_ELDERLY -> "어르신 안전구역 설정"
            mode == PlaceSettingMode.SAFE_ZONE -> "안전구역 설정"
            homeTarget == HomeSettingTarget.PAIRED_ELDERLY -> "어르신 집 설정"
            else -> "집 설정"
        }
    }

    private fun makeSaveButtonText(
        mode: PlaceSettingMode,
        homeTarget: HomeSettingTarget
    ): String {
        return when {
            mode == PlaceSettingMode.SAFE_ZONE &&
                homeTarget == HomeSettingTarget.PAIRED_ELDERLY -> "어르신 안전구역 저장"
            mode == PlaceSettingMode.SAFE_ZONE -> "안전구역 저장"
            homeTarget == HomeSettingTarget.PAIRED_ELDERLY -> "어르신 집 위치 저장"
            else -> "집 위치 저장"
        }
    }

    private fun updateRadiusText() {
        radiusValueText.text = "안전 반경: ${radiusMeters}m"
    }

    private fun setLoading(isLoading: Boolean) {
        saveButton.isEnabled = !isLoading
        saveButton.alpha = if (isLoading) 0.6f else 1.0f
    }

    private enum class PlaceSettingMode {
        HOME,
        SAFE_ZONE
    }

    companion object {
        private const val MIN_RADIUS_METERS = 50
        private const val DEFAULT_RADIUS_METERS = 500
    }
}
