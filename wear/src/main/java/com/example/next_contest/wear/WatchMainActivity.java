package com.example.next_contest.wear;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.next_contest.wear.data.AuthRepository;
import com.example.next_contest.wear.data.LocationSharingRepository;
import com.example.next_contest.wear.data.PairingRepository;
import com.example.next_contest.wear.data.RouteRepository;
import com.example.next_contest.wear.data.UserPlaceRepository;
import com.example.next_contest.wear.model.NavStep;
import com.example.next_contest.wear.model.SavedPlace;
import com.example.next_contest.wear.util.GeoUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WatchMainActivity extends Activity {
    private static final int LOCATION_PERMISSION_REQUEST = 4101;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 4102;
    private static final long NAVIGATION_UPDATE_INTERVAL_MS = 3000L;
    private static final long NAVIGATION_FASTEST_INTERVAL_MS = 1500L;
    private static final float APPROACH_DISTANCE_METERS = 100f;
    private static final float ACTION_DISTANCE_METERS = 20f;
    private static final float ARRIVAL_DISTANCE_METERS = 15f;
    private static final String POLICE_PHONE_NUMBER = "112";

    private final AuthRepository authRepository = new AuthRepository();
    private final UserPlaceRepository userPlaceRepository = new UserPlaceRepository();
    private final LocationSharingRepository locationSharingRepository = new LocationSharingRepository();
    private final RouteRepository routeRepository = new RouteRepository();
    private final PairingRepository pairingRepository = new PairingRepository();

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback navigationLocationCallback;
    private TextToSpeech textToSpeech;
    private Screen currentScreen = Screen.LOGIN;
    private SavedPlace homePlace;
    private List<NavStep> navSteps = new ArrayList<>();
    private int currentStepIndex;
    private int approachSpokenStepIndex;
    private int actionSpokenStepIndex;
    private boolean routeLoaded;
    private boolean routeFallbackMode;
    private boolean arrivedSpoken;
    private boolean pendingNavigationStart;
    private String lastSpokenMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.KOREAN);
            }
        });

        if (authRepository.hasCurrentUser()) {
            showLoginScreen("로그인 상태 확인 중...");
            authRepository.requireCurrentElderly(new AuthRepository.RoleCallback() {
                @Override
                public void onSuccess(String role) {
                    runOnUiThread(() -> showMainScreen());
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(() -> showLoginScreen(message));
                }
            });
        } else {
            showLoginScreen(null);
        }
    }

    private void showLoginScreen(String initialMessage) {
        currentScreen = Screen.LOGIN;
        setContentView(R.layout.activity_watch_login);

        TextView statusView = findViewById(R.id.tvLoginStatus);
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            statusView.setText(initialMessage);
        }

        Button loginButton = findViewById(R.id.btnLogin);
        loginButton.setOnClickListener(view -> handleLogin());
    }

    private void handleLogin() {
        EditText emailView = findViewById(R.id.etEmail);
        EditText passwordView = findViewById(R.id.etPassword);
        Button loginButton = findViewById(R.id.btnLogin);
        TextView statusView = findViewById(R.id.tvLoginStatus);

        String email = emailView.getText().toString().trim();
        String password = passwordView.getText().toString();

        if (email.isEmpty()) {
            statusView.setText("이메일을 입력하세요.");
            return;
        }

        if (password.isEmpty()) {
            statusView.setText("비밀번호를 입력하세요.");
            return;
        }

        setButtonLoading(loginButton, true);
        statusView.setText("로그인 중...");

        authRepository.signInElderlyOnly(email, password, new AuthRepository.RoleCallback() {
            @Override
            public void onSuccess(String role) {
                runOnUiThread(() -> {
                    setButtonLoading(loginButton, false);
                    showMainScreen();
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setButtonLoading(loginButton, false);
                    statusView.setText(message);
                    Toast.makeText(WatchMainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showMainScreen() {
        currentScreen = Screen.MAIN;
        setContentView(R.layout.activity_watch_main);

        findViewById(R.id.btnStartNavigation).setOnClickListener(view -> startHomeNavigation());
        findViewById(R.id.btnStopNavigation).setOnClickListener(view -> stopHomeNavigation(true));
        findViewById(R.id.btnRepeat).setOnClickListener(view -> repeatLastMessage());
        findViewById(R.id.btnHelp).setOnClickListener(view -> showHelpScreen());
        findViewById(R.id.btnLogout).setOnClickListener(view -> logout());

        setNavigationButtons(false);
        loadHomePlace();
        ensureLocationSharing();
        requestNotificationPermissionIfNeeded();
    }

    private void showHelpScreen() {
        stopHomeNavigation(false);
        currentScreen = Screen.HELP;
        setContentView(R.layout.activity_watch_help);

        findViewById(R.id.btnHelpBack).setOnClickListener(view -> showMainScreen());
        findViewById(R.id.btnWatchCallCaregiver).setOnClickListener(view -> callConnectedCaregiver());
        findViewById(R.id.btnWatchCallPolice).setOnClickListener(view -> openDialer(POLICE_PHONE_NUMBER));
    }

    private void callConnectedCaregiver() {
        setHelpStatusText("보호자 번호 확인 중...");

        pairingRepository.loadConnectedCaregiver(new PairingRepository.ConnectedUserCallback() {
            @Override
            public void onSuccess(PairingRepository.ConnectedUser user) {
                runOnUiThread(() -> {
                    String phoneNumber = sanitizePhoneNumber(user.phoneNumber);

                    if (phoneNumber.isEmpty()) {
                        setHelpStatusText("보호자 전화번호를 찾지 못했습니다.");
                        return;
                    }

                    setHelpStatusText(user.name + " 보호자에게 연결합니다.");
                    openDialer(phoneNumber);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setHelpStatusText(message);
                    Toast.makeText(WatchMainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String sanitizePhoneNumber(String phoneNumber) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < phoneNumber.length(); i++) {
            char value = phoneNumber.charAt(i);
            if (Character.isDigit(value) || value == '+') {
                builder.append(value);
            }
        }

        return builder.toString();
    }

    private void openDialer(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));

        try {
            startActivity(intent);
        } catch (Exception error) {
            String message = "전화 앱을 열 수 없습니다.";
            setHelpStatusText(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHomePlace() {
        setHomeAddressText("집 위치 확인 중");
        setInstructionText("폰 앱에서 저장한 집 위치를 불러오는 중입니다.");

        userPlaceRepository.loadHome(new UserPlaceRepository.PlaceCallback() {
            @Override
            public void onSuccess(SavedPlace place) {
                runOnUiThread(() -> {
                    homePlace = place;

                    if (homePlace == null) {
                        setHomeAddressText("집 위치 없음");
                        setInstructionText("폰 앱에서 집 위치를 먼저 설정하세요.");
                        setStartButtonEnabled(false);
                        return;
                    }

                    setHomeAddressText(homePlace.address);
                    setInstructionText("집 안내를 시작할 수 있습니다.");
                    setStartButtonEnabled(true);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setHomeAddressText("집 위치 오류");
                    setInstructionText(message);
                    setStartButtonEnabled(false);
                });
            }
        });
    }

    private void ensureLocationSharing() {
        if (!hasLocationPermission()) {
            setTrackingStatusText("위치 권한이 필요합니다.");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        startLocationService();
        setTrackingStatusText("위치 공유 중");
    }

    private void startLocationService() {
        Intent intent = new Intent(this, WatchLocationService.class);
        intent.setAction(WatchLocationService.ACTION_START);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (SecurityException error) {
            setTrackingStatusText("위치 공유 권한 오류");
        }
    }

    private void stopLocationService() {
        Intent intent = new Intent(this, WatchLocationService.class);
        intent.setAction(WatchLocationService.ACTION_STOP);
        startService(intent);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST
        );
    }

    private void startHomeNavigation() {
        if (homePlace == null) {
            setInstructionText("폰 앱에서 집 위치를 먼저 설정하세요.");
            return;
        }

        if (!hasLocationPermission()) {
            pendingNavigationStart = true;
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        resetNavigationState();
        setNavigationButtons(true);
        speak("집 안내를 시작합니다.");
        setInstructionText("현재 위치를 확인하는 중입니다.");

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                NAVIGATION_UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(NAVIGATION_FASTEST_INTERVAL_MS)
                .build();

        navigationLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    updateNavigation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    navigationLocationCallback,
                    Looper.getMainLooper()
            );

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateNavigation(location);
                        }
                    });
        } catch (SecurityException error) {
            setInstructionText("위치 권한을 확인하지 못했습니다.");
            setNavigationButtons(false);
            navigationLocationCallback = null;
        }
    }

    private void stopHomeNavigation(boolean announce) {
        if (navigationLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(navigationLocationCallback);
            navigationLocationCallback = null;
        }

        setNavigationButtons(false);

        if (announce) {
            speak("집 안내를 종료합니다.");
            setInstructionText("집 안내가 종료되었습니다.");
            setDirectionText("안내 대기");
        }
    }

    private void resetNavigationState() {
        currentStepIndex = 0;
        approachSpokenStepIndex = -1;
        actionSpokenStepIndex = -1;
        routeLoaded = false;
        routeFallbackMode = false;
        arrivedSpoken = false;
        navSteps = new ArrayList<>();
    }

    private void updateNavigation(Location location) {
        if (homePlace == null) {
            return;
        }

        float destinationDistance = GeoUtils.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                homePlace.latitude,
                homePlace.longitude
        );
        float bearing = GeoUtils.bearing(
                location.getLatitude(),
                location.getLongitude(),
                homePlace.latitude,
                homePlace.longitude
        );

        setDistanceText(formatDistance(destinationDistance));
        setDirectionText(GeoUtils.bearingToDirectionText(bearing) + " 방향");

        if (!routeLoaded) {
            routeLoaded = true;
            loadRouteSteps(location);
        }

        if (destinationDistance <= ARRIVAL_DISTANCE_METERS) {
            if (!arrivedSpoken) {
                arrivedSpoken = true;
                speak("집에 도착했습니다.");
                setInstructionText("집에 도착했습니다.");
                stopHomeNavigation(false);
            }
            return;
        }

        if (navSteps.isEmpty()) {
            setInstructionText("경로를 계산하는 중입니다.");
            return;
        }

        if (routeFallbackMode || currentStepIndex >= navSteps.size()) {
            setInstructionText("집 방향으로 이동하세요.");
            return;
        }

        NavStep step = navSteps.get(currentStepIndex);
        float stepDistance = GeoUtils.distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                step.latitude,
                step.longitude
        );

        if (stepDistance <= APPROACH_DISTANCE_METERS) {
            setInstructionText("잠시 후 " + step.instruction);
        } else {
            setInstructionText(step.instruction + "\n다음 안내까지 " + formatDistance(stepDistance));
        }

        if (stepDistance <= ACTION_DISTANCE_METERS && actionSpokenStepIndex != currentStepIndex) {
            actionSpokenStepIndex = currentStepIndex;
            speak(step.instruction);
            currentStepIndex++;
            return;
        }

        if (stepDistance <= APPROACH_DISTANCE_METERS && approachSpokenStepIndex != currentStepIndex) {
            approachSpokenStepIndex = currentStepIndex;
            speak("잠시 후 " + step.instruction);
        }
    }

    private void loadRouteSteps(Location location) {
        String apiKey = BuildConfig.TMAP_API_KEY;

        if (apiKey == null || apiKey.trim().isEmpty()) {
            applyRouteFallback("Tmap 키가 없어 직선 방향 안내로 진행합니다.");
            return;
        }

        new Thread(() -> {
            try {
                List<NavStep> steps = routeRepository.requestRouteSteps(
                        location.getLatitude(),
                        location.getLongitude(),
                        homePlace.latitude,
                        homePlace.longitude,
                        apiKey
                );

                runOnUiThread(() -> {
                    if (steps.isEmpty()) {
                        applyRouteFallback("경로 안내가 없어 직선 방향 안내로 진행합니다.");
                        return;
                    }

                    routeFallbackMode = false;
                    navSteps = steps;
                    setInstructionText("경로를 찾았습니다. 안내를 시작합니다.");
                });
            } catch (Exception error) {
                runOnUiThread(() -> applyRouteFallback("경로 요청 실패: " + error.getMessage()));
            }
        }).start();
    }

    private void applyRouteFallback(String message) {
        routeFallbackMode = true;
        navSteps = Collections.singletonList(new NavStep(
                "집 방향으로 이동하세요.",
                homePlace.latitude,
                homePlace.longitude,
                0
        ));
        setInstructionText(message);
    }

    private void logout() {
        stopHomeNavigation(false);
        locationSharingRepository.markCurrentUserOffline();
        stopLocationService();
        authRepository.signOut();
        homePlace = null;
        showLoginScreen("로그아웃되었습니다.");
    }

    private void repeatLastMessage() {
        if (lastSpokenMessage.trim().isEmpty()) {
            Toast.makeText(this, "다시 들을 안내가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        speak(lastSpokenMessage);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setNavigationButtons(boolean navigationActive) {
        setStartButtonEnabled(!navigationActive && homePlace != null);

        Button stopButton = findViewById(R.id.btnStopNavigation);
        if (stopButton != null) {
            stopButton.setEnabled(navigationActive);
            stopButton.setAlpha(navigationActive ? 1.0f : 0.45f);
        }
    }

    private void setStartButtonEnabled(boolean enabled) {
        Button startButton = findViewById(R.id.btnStartNavigation);
        if (startButton == null) {
            return;
        }

        startButton.setEnabled(enabled);
        startButton.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void setButtonLoading(Button button, boolean isLoading) {
        button.setEnabled(!isLoading);
        button.setAlpha(isLoading ? 0.55f : 1.0f);
    }

    private void setHomeAddressText(String text) {
        TextView view = findViewById(R.id.tvHomeAddress);
        if (view != null) {
            view.setText(text);
        }
    }

    private void setTrackingStatusText(String text) {
        TextView view = findViewById(R.id.tvTrackingStatus);
        if (view != null) {
            view.setText(text);
        }
    }

    private void setDistanceText(String text) {
        TextView view = findViewById(R.id.tvDistance);
        if (view != null) {
            view.setText(text);
        }
    }

    private void setDirectionText(String text) {
        TextView view = findViewById(R.id.tvDirection);
        if (view != null) {
            view.setText(text);
        }
    }

    private void setInstructionText(String text) {
        TextView view = findViewById(R.id.tvInstruction);
        if (view != null) {
            view.setText(text);
        }
    }

    private void setHelpStatusText(String text) {
        TextView view = findViewById(R.id.tvHelpStatus);
        if (view != null) {
            view.setText(text);
        }
    }

    private String formatDistance(float distanceMeters) {
        if (distanceMeters >= 1000f) {
            return String.format(Locale.KOREA, "%.1f km", distanceMeters / 1000f);
        }

        return String.format(Locale.KOREA, "%d m", Math.max(0, Math.round(distanceMeters)));
    }

    private void speak(String message) {
        lastSpokenMessage = message;

        if (textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "watch-home-navigation");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (!granted) {
                pendingNavigationStart = false;
                setTrackingStatusText("위치 권한이 꺼져 있습니다.");
                setInstructionText("위치 권한을 허용해야 위치 공유와 집 안내를 사용할 수 있습니다.");
                return;
            }

            ensureLocationSharing();

            if (pendingNavigationStart) {
                pendingNavigationStart = false;
                startHomeNavigation();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == Screen.HELP) {
            showMainScreen();
        } else if (navigationLocationCallback != null) {
            stopHomeNavigation(true);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onDestroy() {
        stopHomeNavigation(false);

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }

    private enum Screen {
        LOGIN,
        MAIN,
        HELP
    }
}
