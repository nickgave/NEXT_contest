package com.example.next_contest.wear.util;

import android.location.Location;

public final class GeoUtils {
    private GeoUtils() {
    }

    public static float distanceMeters(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        float[] result = new float[1];
        Location.distanceBetween(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                result
        );
        return result[0];
    }

    public static float bearing(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        double startLatRad = Math.toRadians(startLatitude);
        double endLatRad = Math.toRadians(endLatitude);
        double deltaLngRad = Math.toRadians(endLongitude - startLongitude);

        double y = Math.sin(deltaLngRad) * Math.cos(endLatRad);
        double x = Math.cos(startLatRad) * Math.sin(endLatRad)
                - Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(deltaLngRad);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (float) ((bearing + 360.0) % 360.0);
    }

    public static String bearingToDirectionText(float bearing) {
        String[] directions = {
                "북쪽",
                "북동쪽",
                "동쪽",
                "남동쪽",
                "남쪽",
                "남서쪽",
                "서쪽",
                "북서쪽"
        };

        int index = (int) ((bearing + 22.5f) / 45.0f) % directions.length;
        return directions[index];
    }
}
