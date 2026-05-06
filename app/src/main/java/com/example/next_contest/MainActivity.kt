package com.example.next_contest

import android.Manifest
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
import com.example.next_contest.controller.SimpleScreenController

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var ttsHelper: TTSHelper
    private lateinit var compassHelper: CompassHelper
    private lateinit var dailyInfoController: DailyInfoController
    private lateinit var homeNavigationController: HomeNavigationController
    private lateinit var authController: AuthController
    private lateinit var mainMenuController: MainMenuController
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
                showMapScreen()
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

    private fun showPatientMainScreen() {
        mainMenuController.showPatientMainScreen()
    }

    private fun showGuardianMainScreen() {
        mainMenuController.showGuardianMainScreen()
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

    private fun showLoginScreen() {
        stopLocationUpdates()
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
        ttsHelper.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_WEATHER = 1001
        private const val LOCATION_PERMISSION_NAVIGATION = 1002
    }
}
