package com.example.next_contest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.example.next_contest.util.CompassHelper
import com.example.next_contest.util.GeoUtils
import com.example.next_contest.util.DirectionUtils
import com.example.next_contest.model.PatientLocation
import com.example.next_contest.data.tracking.TrackingRepository
import com.example.next_contest.controller.TracingUiController
import com.example.next_contest.controller.GuardianLocationController
import com.example.next_contest.util.applySystemBarInsetsToContent

class TracingActivity : AppCompatActivity() {
    private val trackingRepository = TrackingRepository()
    private lateinit var uiController: TracingUiController
    private lateinit var guardianLocationController: GuardianLocationController
    private var guardianLat: Double? = null
    private var guardianLng: Double? = null
    private lateinit var compassHelper: CompassHelper
    private var currentDegree = 0f
    private var patientLat: Double? = null
    private var patientLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_tracing)

        compassHelper = CompassHelper(this) { degree ->
            currentDegree = degree
            updateArrowAndDistance()
        }

        guardianLocationController = GuardianLocationController(
            activity = this,
            requestLocationPermission = {
                requestTrackingLocationPermission()
            },
            onLocationChanged = { lat, lng ->
                guardianLat = lat
                guardianLng = lng
                updateMap()
                updateArrowAndDistance()
            }
        )

        uiController = TracingUiController(this)
        uiController.bindViews()

        setupBackButton()
        setupDismissSosButton()
        startGuardianLocationUpdates()
        startWatchingPatientLocation()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applySystemBarInsetsToContent()
    }

    private fun setupBackButton() {
        uiController.setBackClickListener {
            finish()
        }
    }

    private fun setupDismissSosButton() {
        uiController.setDismissSosClickListener {
            trackingRepository.dismissSos(
                onSuccess = {
                    uiController.hideSosBanner()
                    Toast.makeText(this, "SOS 알림을 해제했습니다.", Toast.LENGTH_SHORT).show()
                },
                onFailure = { message ->
                    Toast.makeText(this, "SOS 해제 실패: $message", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun requestTrackingLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_TRACKING
        )
    }

    private fun startGuardianLocationUpdates() {
        guardianLocationController.start()
    }

    private fun stopGuardianLocationUpdates() {
        if (::guardianLocationController.isInitialized) {
            guardianLocationController.stop()
        }
    }

    private fun startWatchingPatientLocation() {
        trackingRepository.startWatchingPatientLocation(
            onNoPatient = { message ->
                uiController.showNoPatientMessage(message)
            },
            onPatientNameLoaded = { name ->
                uiController.showPatientName(name)
            },
            onLocationChanged = { patientLocation ->
                showPatientLocation(patientLocation)
            },
            onLocationMissing = {
                uiController.showNoPatientMessage("아직 위치 데이터가 없습니다.")
            },
            onError = { message ->
                Toast.makeText(
                    this@TracingActivity,
                    "위치 수신 오류: $message",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun stopWatchingPatientLocation() {
        trackingRepository.stopWatchingPatientLocation()
    }

    private fun showPatientLocation(location: PatientLocation) {
        patientLat = location.latitude
        patientLng = location.longitude

        uiController.showPatientLocation(location)

        updateMap()
        updateArrowAndDistance()
    }

    override fun onResume() {
        super.onResume()
        if (::compassHelper.isInitialized) {
            compassHelper.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::compassHelper.isInitialized) {
            compassHelper.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWatchingPatientLocation()
        stopGuardianLocationUpdates()
        if (::uiController.isInitialized) {
            uiController.stopMap()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::uiController.isInitialized) {
            uiController.onLowMemory()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_TRACKING) return

        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startGuardianLocationUpdates()
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateArrowAndDistance() {
        val gLat = guardianLat ?: return
        val gLng = guardianLng ?: return
        val pLat = patientLat ?: return
        val pLng = patientLng ?: return

        val distance = GeoUtils.distanceBetween(gLat, gLng, pLat, pLng)
        val bearing = GeoUtils.getBearing(gLat, gLng, pLat, pLng)
        val correctedBearing = bearing - currentDegree

        uiController.showDistanceAndDirection(
            distanceMeters = distance,
            directionText = DirectionUtils.getPatientDirectionCaption(correctedBearing),
            arrowRotation = correctedBearing
        )
    }

    private fun updateMap() {
        uiController.showMapLocations(
            guardianLat = guardianLat,
            guardianLng = guardianLng,
            patientLat = patientLat,
            patientLng = patientLng
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_TRACKING = 2001
    }
}
