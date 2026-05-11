package com.example.next_contest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.next_contest.model.UserRole
import com.example.next_contest.util.TTSHelper
import com.example.next_contest.util.CompassHelper
import com.example.next_contest.data.weather.WeatherRepository
import com.example.next_contest.data.route.RouteRepository
import com.example.next_contest.controller.DailyInfoController
import com.example.next_contest.controller.HomeNavigationController
import com.example.next_contest.controller.AuthController
import com.example.next_contest.controller.MainMenuController
import com.example.next_contest.controller.PairedLocationMapController
import com.example.next_contest.controller.PatientLocationShareController
import com.example.next_contest.controller.SimpleScreenController

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var ttsHelper: TTSHelper
    private lateinit var compassHelper: CompassHelper
    private lateinit var dailyInfoController: DailyInfoController
    private lateinit var homeNavigationController: HomeNavigationController
    private lateinit var authController: AuthController
    private lateinit var mainMenuController: MainMenuController
    private lateinit var pairedLocationMapController: PairedLocationMapController
    private lateinit var patientLocationShareController: PatientLocationShareController
    private lateinit var simpleScreenController: SimpleScreenController
    private var currentDegree = 0f
    private var userRole: UserRole = UserRole.ELDERLY

    private val destinationLat = 37.5872
    private val destinationLng = 127.0315

    private val weatherRepository = WeatherRepository()
    private val routeRepository = RouteRepository(
        destinationLat = destinationLat,
        destinationLng = destinationLng
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initHelpers()
        initControllers()

        showLoginScreen()
    }

    private fun initHelpers() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ttsHelper = TTSHelper(this)
        ttsHelper.init()

        compassHelper = CompassHelper(this) { degree ->
            currentDegree = degree
        }
    }

    private fun initControllers() {
        initAuthController()
        initDailyInfoController()
        initHomeNavigationController()
        initMainMenuController()
        initPatientLocationShareController()
        initPairedLocationMapController()
        initSimpleScreenController()
    }

    private fun initAuthController() {
        authController = AuthController(
            activity = this,
            onLoginAsPatient = {
                userRole = UserRole.ELDERLY
                showPatientMainScreen()
            },
            onLoginAsGuardian = {
                userRole = UserRole.GUARDIAN
                showGuardianMainScreen()
            }
        )
    }

    private fun initDailyInfoController() {
        dailyInfoController = DailyInfoController(
            activity = this,
            weatherRepository = weatherRepository,
            requestLocationPermission = {
                requestWeatherLocationPermission()
            },
            onBackToPatientMain = {
                showPatientMainScreen()
            }
        )
    }

    private fun initHomeNavigationController() {
        homeNavigationController = HomeNavigationController(
            activity = this,
            fusedLocationClient = fusedLocationClient,
            ttsHelper = ttsHelper,
            routeRepository = routeRepository,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            currentDegreeProvider = {
                currentDegree
            },
            requestLocationPermission = {
                requestNavigationLocationPermission()
            },
            onBackToPatientMain = {
                showPatientMainScreen()
            }
        )
    }

    private fun initMainMenuController() {
        mainMenuController = MainMenuController(
            activity = this,
            stopNavigation = {
                stopLocationUpdates()
            },
            onNavigateHome = {
                startNavigation()
            },
            onShowDailyInfo = {
                showDailyInfo()
            },
            onShowMap = {
                showLocationScreen()
            },
            onShowHelp = {
                showHelpScreen()
            },
            onShowSettings = {
                showSettingsScreen()
            },
            onShowSafeZone = {
                showSafeZoneScreen()
            },
            onLogout = {
                showLoginScreen()
            }
        )
    }

    private fun initSimpleScreenController() {
        simpleScreenController = SimpleScreenController(
            activity = this,
            stopNavigation = {
                stopLocationUpdates()
            },
            getUserRole = {
                userRole
            },
            onBackToPatientMain = {
                showPatientMainScreen()
            },
            onBackToGuardianMain = {
                showGuardianMainScreen()
            },
            pairedLocationMapController = pairedLocationMapController
        )
    }

    private fun initPatientLocationShareController() {
        patientLocationShareController = PatientLocationShareController(
            activity = this,
            fusedLocationClient = fusedLocationClient,
            requestLocationPermission = {
                requestPatientLocationSharingPermission()
            }
        )
    }

    private fun initPairedLocationMapController() {
        pairedLocationMapController = PairedLocationMapController(
            activity = this,
            fusedLocationClient = fusedLocationClient,
            getUserRole = {
                userRole
            },
            requestLocationPermission = {
                requestPairedLocationMapPermission()
            }
        )
    }

    private fun requestWeatherLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_WEATHER
        )
    }

    private fun requestNavigationLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_NAVIGATION
        )
    }

    private fun requestPatientLocationSharingPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_PATIENT_SHARING
        )
    }

    private fun requestPairedLocationMapPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_PAIRED_MAP
        )
    }

    private fun showPatientMainScreen() {
        mainMenuController.showPatientMainScreen()
        startPatientLocationSharing()
    }

    private fun showGuardianMainScreen() {
        mainMenuController.showGuardianMainScreen()
        startPatientLocationSharing()
    }

    private fun showSettingsScreen() {
        simpleScreenController.showSettingsScreen()
    }

    private fun showSafeZoneScreen() {
        simpleScreenController.showSafeZoneScreen()
    }

    private fun showMapScreen() {
        simpleScreenController.showMapScreen()
    }

    private fun showLocationScreen() {
        if (userRole == UserRole.GUARDIAN) {
            showTracingScreen()
        } else {
            showMapScreen()
        }
    }

    private fun showTracingScreen() {
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        startActivity(Intent(this, TracingActivity::class.java))
    }

    private fun showLoginScreen() {
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        stopPatientLocationSharing()
        authController.showLoginScreen()
    }

    private fun showDailyInfo() {
        homeNavigationController.stopLocationUpdates()
        dailyInfoController.show()
    }

    private fun showHelpScreen() {
        simpleScreenController.showHelpScreen()
    }

    private fun startNavigation() {
        homeNavigationController.start()
    }

    private fun stopLocationUpdates() {
        if (::homeNavigationController.isInitialized) {
            homeNavigationController.stopLocationUpdates()
        }
    }

    private fun startPatientLocationSharing() {
        if (::patientLocationShareController.isInitialized) {
            patientLocationShareController.start()
        }
    }

    private fun stopPatientLocationSharing() {
        if (::patientLocationShareController.isInitialized) {
            patientLocationShareController.stop()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        when (requestCode) {
            LOCATION_PERMISSION_WEATHER -> {
                dailyInfoController.fetchWeatherUsingCurrentPhoneLocation()
            }

            LOCATION_PERMISSION_NAVIGATION -> {
                homeNavigationController.start()
            }

            LOCATION_PERMISSION_PATIENT_SHARING -> {
                startPatientLocationSharing()
            }

            LOCATION_PERMISSION_PAIRED_MAP -> {
                pairedLocationMapController.start()
            }
        }
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
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        stopPatientLocationSharing()
        ttsHelper.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_WEATHER = 1001
        private const val LOCATION_PERMISSION_NAVIGATION = 1002
        private const val LOCATION_PERMISSION_PATIENT_SHARING = 1003
        private const val LOCATION_PERMISSION_PAIRED_MAP = 1004
    }
}
