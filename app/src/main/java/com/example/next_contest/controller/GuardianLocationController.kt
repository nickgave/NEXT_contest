package com.example.next_contest.controller

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class GuardianLocationController(
    private val activity: AppCompatActivity,
    private val requestLocationPermission: () -> Unit,
    private val onLocationChanged: (lat: Double, lng: Double) -> Unit
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    private var locationCallback: LocationCallback? = null

    fun start() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(3000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                onLocationChanged(
                    location.latitude,
                    location.longitude
                )
            }
        }

        try {
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    request,
                    callback,
                    activity.mainLooper
                )
            }
        } catch (e: SecurityException) {
            Toast.makeText(activity, "위치 권한 오류", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        locationCallback = null
    }
}