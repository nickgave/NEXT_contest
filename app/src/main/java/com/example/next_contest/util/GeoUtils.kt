package com.example.next_contest.util

import android.location.Location

object GeoUtils {

    fun getBearing(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val dLon = Math.toRadians(lng2 - lng1)

        val y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2))

        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.cos(dLon)

        val bearing = Math.toDegrees(Math.atan2(y, x))

        return ((bearing + 360) % 360).toFloat()
    }

    fun distanceBetween(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
}