package com.example.next_contest.controller

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.next_contest.data.tracking.LocationSharingRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class PatientLocationShareController(
    private val activity: AppCompatActivity,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val requestLocationPermission: () -> Unit,
    private val locationSharingRepository: LocationSharingRepository = LocationSharingRepository()
) {
    private var locationCallback: LocationCallback? = null

    fun start() {
        if (locationCallback != null) return

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        locationSharingRepository.ensureCurrentUserLocationNode(
            onFailure = { message ->
                Log.e(TAG, "Failed to initialize location node: $message")
            }
        )

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                locationSharingRepository.updateCurrentUserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    onFailure = { message ->
                        Log.e(TAG, "Failed to share patient location: $message")
                    }
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                activity.mainLooper
            )
        } catch (e: SecurityException) {
            locationCallback = null
            Toast.makeText(activity, "위치 권한 오류", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }

        locationCallback = null
        locationSharingRepository.markCurrentUserOffline()
    }

    companion object {
        private const val TAG = "PatientLocationShare"
        private const val LOCATION_UPDATE_INTERVAL_MS = 5000L
        private const val LOCATION_FASTEST_INTERVAL_MS = 3000L
    }
}
