package com.example.next_contest.wear;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.next_contest.wear.data.LocationSharingRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class WatchLocationService extends Service {
    public static final String ACTION_START = "com.example.next_contest.wear.action.START_LOCATION";
    public static final String ACTION_STOP = "com.example.next_contest.wear.action.STOP_LOCATION";

    private static final String TAG = "WatchLocationService";
    private static final String CHANNEL_ID = "watch_location_tracking";
    private static final int NOTIFICATION_ID = 3100;
    private static final long LOCATION_UPDATE_INTERVAL_MS = 5000L;
    private static final long LOCATION_FASTEST_INTERVAL_MS = 3000L;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationSharingRepository locationSharingRepository;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationSharingRepository = new LocationSharingRepository();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopSharing(true);
            stopForegroundCompat();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startSharing();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startSharing() {
        if (locationCallback != null) {
            return;
        }

        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission is missing.");
            stopSelf();
            return;
        }

        locationSharingRepository.ensureCurrentUserLocationNode(message ->
                Log.e(TAG, "Failed to initialize location node: " + message)
        );

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL_MS
        )
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL_MS)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result.getLastLocation() == null) {
                    return;
                }

                locationSharingRepository.updateCurrentUserLocation(
                        result.getLastLocation().getLatitude(),
                        result.getLastLocation().getLongitude(),
                        message -> Log.e(TAG, "Failed to share location: " + message)
                );
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException error) {
            Log.e(TAG, "Location permission was revoked.", error);
            locationCallback = null;
            stopSelf();
        }
    }

    private void stopSharing(boolean markOffline) {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        if (markOffline) {
            locationSharingRepository.markCurrentUserOffline();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, WatchMainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_location)
                .setContentTitle("NEXT Watch")
                .setContentText("어르신 위치를 보호자에게 공유 중입니다.")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "위치 공유",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("어르신 위치 공유 상태를 표시합니다.");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    @Override
    public void onDestroy() {
        stopSharing(true);
        super.onDestroy();
    }
}
