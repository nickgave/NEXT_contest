package com.example.next_contest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.next_contest.controller.PlaceSettingController
import com.example.next_contest.controller.SimpleScreenController
import com.example.next_contest.model.SavedPlace
import com.example.next_contest.service.PlaceService

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
    private lateinit var placeSettingController: PlaceSettingController
    private lateinit var simpleScreenController: SimpleScreenController
    private var currentDegree = 0f
    private var userRole: UserRole = UserRole.ELDERLY
    private var currentScreen: AppScreen = AppScreen.LOGIN
    private var lastMainBackPressedAt = 0L
    private var homePlace: SavedPlace? = null

    private val defaultHomePlace = SavedPlace(
        latitude = 37.5872,
        longitude = 127.0315,
        address = "기본 집 위치"
    )

    private val weatherRepository = WeatherRepository()
    private val routeRepository = RouteRepository()
    private val placeService = PlaceService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initHelpers()
        initControllers()
        configureBackNavigation()

        showLoginScreen()
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPressed()
                }
            }
        )
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
        initPlaceSettingController()
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
            onShowHomeSetting = {
                showHomeSettingScreen()
            },
            pairedLocationMapController = pairedLocationMapController
        )
    }

    private fun initPlaceSettingController() {
        placeSettingController = PlaceSettingController(
            activity = this,
            placeService = placeService
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
        currentScreen = AppScreen.PATIENT_MAIN
        resetMainBackExitTimer()
        mainMenuController.showPatientMainScreen()
        loadHomePlace()
        startPatientLocationSharing()
    }

    private fun showGuardianMainScreen() {
        currentScreen = AppScreen.GUARDIAN_MAIN
        resetMainBackExitTimer()
        mainMenuController.showGuardianMainScreen()
        loadHomePlace()
        startPatientLocationSharing()
    }

    private fun showSettingsScreen() {
        currentScreen = AppScreen.SETTINGS
        simpleScreenController.showSettingsScreen()
    }

    private fun showSafeZoneScreen() {
        currentScreen = AppScreen.SAFE_ZONE
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        placeSettingController.showSafeZoneSetting(
            defaultPlace = homePlace ?: defaultHomePlace,
            onBack = {
                showGuardianMainScreen()
            },
            onSaved = {
                // Safe zone is persisted by PlaceSettingController.
            }
        )
    }

    private fun showMapScreen() {
        currentScreen = AppScreen.MAP
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
        currentScreen = AppScreen.LOGIN
        resetMainBackExitTimer()
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        stopPatientLocationSharing()
        authController.showLoginScreen()
    }

    private fun showDailyInfo() {
        currentScreen = AppScreen.DAILY_INFO
        homeNavigationController.stopLocationUpdates()
        dailyInfoController.show()
    }

    private fun showHelpScreen() {
        currentScreen = AppScreen.HELP
        simpleScreenController.showHelpScreen()
    }

    private fun startNavigation() {
        currentScreen = AppScreen.HOME_NAVIGATION
        placeService.loadHome(
            onSuccess = { savedHome ->
                runOnUiThread {
                    homePlace = savedHome
                    homeNavigationController.start(savedHome ?: defaultHomePlace)
                }
            },
            onFailure = { message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    homeNavigationController.start(homePlace ?: defaultHomePlace)
                }
            }
        )
    }

    private fun showHomeSettingScreen() {
        currentScreen = AppScreen.HOME_SETTING
        stopLocationUpdates()
        if (::pairedLocationMapController.isInitialized) {
            pairedLocationMapController.stop()
        }
        placeSettingController.showHomeSetting(
            defaultPlace = homePlace ?: defaultHomePlace,
            onBack = {
                showSettingsScreen()
            },
            onSaved = { savedHome ->
                homePlace = savedHome
            }
        )
    }

    private fun loadHomePlace() {
        placeService.loadHome(
            onSuccess = { savedHome ->
                homePlace = savedHome
            },
            onFailure = {
                // Home has a fallback; no need to interrupt the main screen.
            }
        )
    }

    private fun handleBackPressed() {
        when (currentScreen) {
            AppScreen.PATIENT_MAIN,
            AppScreen.GUARDIAN_MAIN -> handleMainBackPressed()

            AppScreen.LOGIN -> {
                Toast.makeText(this, "메인 화면에서만 뒤로가기로 종료할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }

            AppScreen.SETTINGS,
            AppScreen.MAP -> {
                if (::pairedLocationMapController.isInitialized) {
                    pairedLocationMapController.stop()
                }
                goBackToMainByRole()
            }

            AppScreen.HOME_SETTING -> {
                if (::placeSettingController.isInitialized) {
                    placeSettingController.stop()
                }
                showSettingsScreen()
            }

            AppScreen.SAFE_ZONE -> {
                if (::placeSettingController.isInitialized) {
                    placeSettingController.stop()
                }
                showGuardianMainScreen()
            }

            AppScreen.DAILY_INFO,
            AppScreen.HOME_NAVIGATION,
            AppScreen.HELP -> showPatientMainScreen()
        }
    }

    private fun handleMainBackPressed() {
        val now = System.currentTimeMillis()

        if (now - lastMainBackPressedAt <= MAIN_BACK_EXIT_INTERVAL_MS) {
            finish()
            return
        }

        lastMainBackPressedAt = now
        Toast.makeText(this, "한 번 더 누르면 앱을 종료합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun goBackToMainByRole() {
        if (userRole == UserRole.GUARDIAN) {
            showGuardianMainScreen()
        } else {
            showPatientMainScreen()
        }
    }

    private fun resetMainBackExitTimer() {
        lastMainBackPressedAt = 0L
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
                startNavigation()
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
        private const val MAIN_BACK_EXIT_INTERVAL_MS = 2000L
    }

    private enum class AppScreen {
        LOGIN,
        PATIENT_MAIN,
        GUARDIAN_MAIN,
        SETTINGS,
        SAFE_ZONE,
        MAP,
        DAILY_INFO,
        HOME_NAVIGATION,
        HELP,
        HOME_SETTING
    }
}
