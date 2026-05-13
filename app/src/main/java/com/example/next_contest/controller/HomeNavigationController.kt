package com.example.next_contest.controller

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.next_contest.BuildConfig
import com.example.next_contest.R
import com.example.next_contest.data.route.RouteRepository
import com.example.next_contest.model.NavStep
import com.example.next_contest.model.SavedPlace
import com.example.next_contest.util.GeoUtils
import com.example.next_contest.util.TTSHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class HomeNavigationController(
    private val activity: AppCompatActivity,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val ttsHelper: TTSHelper,
    private val routeRepository: RouteRepository,
    private val currentDegreeProvider: () -> Float,
    private val requestLocationPermission: () -> Unit,
    private val onBackToPatientMain: () -> Unit,
    private val onShowHelp: () -> Unit
) {
    private var locationCallback: LocationCallback? = null

    private var currentStepIndex = 0
    private var approachSpokenStepIndex = -1
    private var actionSpokenStepIndex = -1
    private var routeLoaded = false
    private var routeFallbackMode = false
    private var arrivedSpoken = false
    private var navSteps: List<NavStep> = emptyList()
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0

    fun start(destination: SavedPlace) {
        destinationLat = destination.latitude
        destinationLng = destination.longitude

        activity.setContentView(R.layout.activity_home_navigation)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            stopLocationUpdates()
            onBackToPatientMain()
        }

        activity.findViewById<Button>(R.id.helpButton).setOnClickListener {
            stopLocationUpdates()
            onShowHelp()
        }

        activity.findViewById<Button>(R.id.repeatButton).setOnClickListener {
            if (ttsHelper.lastSpokenMessage.isNotBlank()) {
                speak(ttsHelper.lastSpokenMessage)
            } else {
                Toast.makeText(activity, "다시 들을 안내가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        resetNavigationState()

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        speak("집으로 안내를 시작합니다.")
        startLocationUpdates()
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun resetNavigationState() {
        currentStepIndex = 0
        approachSpokenStepIndex = -1
        actionSpokenStepIndex = -1
        routeLoaded = false
        routeFallbackMode = false
        arrivedSpoken = false
        navSteps = emptyList()
        ttsHelper.clearLastMessage()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                updateArrow(location)

                if (!routeLoaded) {
                    routeLoaded = true
                    loadRouteSteps(location)
                }

                handleNavigationStep(location)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                activity.mainLooper
            )
        }
    }

    private fun updateArrow(location: Location) {
        val arrow = activity.findViewById<ImageView?>(R.id.arrow) ?: return

        val bearing = GeoUtils.getBearing(
            location.latitude,
            location.longitude,
            destinationLat,
            destinationLng
        )

        arrow.rotation = bearing - currentDegreeProvider()
    }

    private fun loadRouteSteps(location: Location) {
        val apiKey = BuildConfig.TMAP_API_KEY

        if (apiKey.isBlank()) {
            routeFallbackMode = true
            navSteps = listOf(
                NavStep("목적지 방향으로 이동하세요.", destinationLat, destinationLng, 0)
            )
            updateNavigationText("API 키가 없어 기본 안내로 진행합니다.")
            return
        }

        Thread {
            try {
                val steps = routeRepository.requestRouteSteps(
                    startLat = location.latitude,
                    startLng = location.longitude,
                    destinationLat = destinationLat,
                    destinationLng = destinationLng,
                    apiKey = apiKey
                )

                activity.runOnUiThread {
                    routeFallbackMode = steps.isEmpty()

                    navSteps = steps.ifEmpty {
                        listOf(
                            NavStep("목적지 방향으로 이동하세요.", destinationLat, destinationLng, 0)
                        )
                    }

                    updateNavigationText(
                        if (steps.isEmpty()) {
                            "경로 단계가 0개입니다. 현재 위치가 한국 안인지 확인하세요."
                        } else {
                            "경로를 찾았습니다. 안내를 시작합니다."
                        },
                        if (steps.isEmpty()) {
                            "목적지 방향 안내"
                        } else {
                            "첫 안내를 준비 중입니다."
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Routes API request failed", e)

                activity.runOnUiThread {
                    routeFallbackMode = true
                    navSteps = listOf(
                        NavStep("목적지 방향으로 이동하세요.", destinationLat, destinationLng, 0)
                    )
                    updateNavigationText("Tmap API 실패: ${e.message ?: "알 수 없는 오류"}")
                }
            }
        }.start()
    }

    private fun handleNavigationStep(location: Location) {
        if (navSteps.isEmpty()) return

        val destinationDistance = GeoUtils.distanceBetween(
            location.latitude,
            location.longitude,
            destinationLat,
            destinationLng
        )

        if (destinationDistance <= ARRIVAL_DISTANCE_METERS) {
            if (!arrivedSpoken) {
                arrivedSpoken = true
                speak("도착했습니다.")
                updateNavigationText("도착했습니다.")
                stopLocationUpdates()
            }
            return
        }

        if (currentStepIndex >= navSteps.size) return

        val step = navSteps[currentStepIndex]

        val stepDistance = GeoUtils.distanceBetween(
            location.latitude,
            location.longitude,
            step.lat,
            step.lng
        )

        if (routeFallbackMode) {
            updateNavigationText(
                "목적지 방향으로 이동하세요.",
                "목적지까지 직선거리 ${destinationDistance.toInt()}m"
            )
            return
        }

        updateNavigationText(
            if (stepDistance <= APPROACH_DISTANCE_METERS) {
                "잠시 후 ${step.instruction}"
            } else {
                step.instruction
            },
            "다음 안내까지 ${stepDistance.toInt()}m"
        )

        if (stepDistance <= ACTION_DISTANCE_METERS &&
            actionSpokenStepIndex != currentStepIndex
        ) {
            actionSpokenStepIndex = currentStepIndex
            speak(step.instruction)
            currentStepIndex++
            return
        }

        if (stepDistance <= APPROACH_DISTANCE_METERS &&
            approachSpokenStepIndex != currentStepIndex
        ) {
            approachSpokenStepIndex = currentStepIndex
            speak("잠시 후 ${step.instruction}")
        }
    }

    private fun updateNavigationText(message: String) {
        activity.findViewById<TextView?>(R.id.tvNavigationCaption)?.text = message
        activity.findViewById<TextView?>(R.id.tvDistanceRemaining)?.text = message
    }

    private fun updateNavigationText(caption: String, distanceStatus: String) {
        activity.findViewById<TextView?>(R.id.tvNavigationCaption)?.text = caption
        activity.findViewById<TextView?>(R.id.tvDistanceRemaining)?.text = distanceStatus
    }

    private fun speak(message: String) {
        ttsHelper.speak(message)
    }

    companion object {
        private const val TAG = "HomeNavigationController"
        private const val APPROACH_DISTANCE_METERS = 100
        private const val ACTION_DISTANCE_METERS = 20
        private const val ARRIVAL_DISTANCE_METERS = 15
    }
}
