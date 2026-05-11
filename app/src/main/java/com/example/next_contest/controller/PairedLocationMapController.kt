package com.example.next_contest.controller

import android.Manifest
import android.content.pm.PackageManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.next_contest.R
import com.example.next_contest.data.tracking.PairedLocationRepository
import com.example.next_contest.model.PatientLocation
import com.example.next_contest.model.UserRole
import com.example.next_contest.util.GeoUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.kakao.vectormap.MapView

class PairedLocationMapController(
    private val activity: AppCompatActivity,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val getUserRole: () -> UserRole,
    private val requestLocationPermission: () -> Unit,
    private val pairedLocationRepository: PairedLocationRepository = PairedLocationRepository()
) {
    private var locationCallback: LocationCallback? = null
    private var myLat: Double? = null
    private var myLng: Double? = null
    private var pairedLat: Double? = null
    private var pairedLng: Double? = null
    private var pairedName: String = "상대방"
    private lateinit var mapController: KakaoLocationMapController
    private lateinit var statusText: TextView
    private lateinit var distanceText: TextView
    private var mapStarted = false
    private var watchingPairedLocation = false

    fun start() {
        if (!mapStarted) {
            mapController = KakaoLocationMapController(
                activity.findViewById<MapView>(R.id.liveMapView)
            )
            mapController.start()
            mapStarted = true
        }
        statusText = activity.findViewById(R.id.tvMapStatus)
        distanceText = activity.findViewById(R.id.tvMapDistance)

        statusText.text = "위치 정보를 불러오는 중입니다."
        distanceText.text = ""

        if (locationCallback == null) {
            startMyLocationUpdates()
        }

        if (!watchingPairedLocation) {
            watchingPairedLocation = true
            startWatchingPairedLocation()
        }
    }

    fun stop() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }

        locationCallback = null
        pairedLocationRepository.stopWatchingPairedLocation()
        watchingPairedLocation = false

        if (::mapController.isInitialized && mapStarted) {
            mapController.stop()
            mapStarted = false
        }
    }

    private fun startMyLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                myLat = location.latitude
                myLng = location.longitude
                updateMap()
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                activity.mainLooper
            )
        } catch (e: SecurityException) {
            Toast.makeText(activity, "위치 권한 오류", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startWatchingPairedLocation() {
        pairedLocationRepository.startWatchingPairedLocation(
            onNoPair = { message ->
                activity.runOnUiThread {
                    statusText.text = message
                    updateMap()
                }
            },
            onPairedNameLoaded = { name ->
                pairedName = name
                activity.runOnUiThread {
                    updateMap()
                }
            },
            onLocationChanged = { location ->
                showPairedLocation(location)
            },
            onLocationMissing = {
                activity.runOnUiThread {
                    statusText.text = "연결된 사용자의 위치 데이터가 아직 없습니다."
                    updateMap()
                }
            },
            onError = { message ->
                activity.runOnUiThread {
                    statusText.text = "상대 위치 수신 오류: $message"
                }
            }
        )
    }

    private fun showPairedLocation(location: PatientLocation) {
        pairedLat = location.latitude
        pairedLng = location.longitude

        activity.runOnUiThread {
            statusText.text =
                if (location.isOnline) {
                    "$pairedName 위치 공유 중"
                } else {
                    "$pairedName 마지막 위치"
                }
            updateMap()
        }
    }

    private fun updateMap() {
        mapController.showLocations(
            meLat = myLat,
            meLng = myLng,
            otherLat = pairedLat,
            otherLng = pairedLng,
            meLabel = "나",
            otherLabel = otherLabel()
        )

        val currentMyLat = myLat
        val currentMyLng = myLng
        val currentPairedLat = pairedLat
        val currentPairedLng = pairedLng

        if (currentMyLat != null && currentMyLng != null &&
            currentPairedLat != null && currentPairedLng != null
        ) {
            val distance = GeoUtils.distanceBetween(
                currentMyLat,
                currentMyLng,
                currentPairedLat,
                currentPairedLng
            )
            distanceText.text = "상대와의 거리: 약 ${distance.toInt()}m"
        } else {
            distanceText.text = "두 위치가 모두 수신되면 거리를 표시합니다."
        }
    }

    private fun otherLabel(): String {
        return if (getUserRole() == UserRole.ELDERLY) {
            "보호자"
        } else {
            "어르신"
        }
    }

    companion object {
        private const val LOCATION_UPDATE_INTERVAL_MS = 5000L
        private const val LOCATION_FASTEST_INTERVAL_MS = 3000L
    }
}
