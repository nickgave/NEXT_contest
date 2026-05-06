package com.example.next_contest.data.route

import com.example.next_contest.model.NavStep
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RouteRepository(
    private val destinationLat: Double,
    private val destinationLng: Double
) {

    fun requestRouteSteps(
        startLat: Double,
        startLng: Double,
        apiKey: String
    ): List<NavStep> {
        val url = URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json")

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("appKey", apiKey)
            doOutput = true
        }

        val body = JSONObject()
            .put("startX", startLng.toString())
            .put("startY", startLat.toString())
            .put("endX", destinationLng.toString())
            .put("endY", destinationLat.toString())
            .put("reqCoordType", "WGS84GEO")
            .put("resCoordType", "WGS84GEO")
            .put("startName", "start")
            .put("endName", "home")
            .toString()

        OutputStreamWriter(connection.outputStream).use {
            it.write(body)
        }

        val response = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            throw IllegalStateException("Tmap API error ${connection.responseCode}: $error")
        }

        if (response.isBlank()) {
            throw IllegalStateException(
                "Tmap API ${connection.responseCode}: 응답이 비어 있습니다. 출발 좌표를 확인하세요."
            )
        }

        return parseRouteSteps(response)
    }

    private fun parseRouteSteps(response: String): List<NavStep> {
        val features = JSONObject(response).optJSONArray("features")
            ?: throw IllegalStateException("Tmap 응답에 features가 없습니다: ${response.take(120)}")

        val result = mutableListOf<NavStep>()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val properties = feature.optJSONObject("properties") ?: continue

            val instruction = properties.optString("description")
                .takeIf { it.isNotBlank() }
                ?: continue

            val geometry = feature.optJSONObject("geometry") ?: continue
            val coordinates = geometry.optJSONArray("coordinates") ?: continue

            val point = when (geometry.optString("type")) {
                "Point" -> coordinates
                "LineString" -> coordinates.optJSONArray(coordinates.length() - 1)
                else -> null
            } ?: continue

            if (point.length() < 2) continue

            result.add(
                NavStep(
                    instruction = instruction,
                    lat = point.getDouble(1),
                    lng = point.getDouble(0),
                    distanceMeters = properties.optInt("distance", 0)
                )
            )
        }

        return result
    }
}