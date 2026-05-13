package com.example.next_contest.data.place

import com.example.next_contest.model.SavedPlace
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class KakaoLocalRepository {

    fun findPlaceByAddress(query: String, apiKey: String): SavedPlace? {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return null

        return searchAddress(trimmedQuery, apiKey)
            ?: searchKeyword(trimmedQuery, apiKey)
    }

    fun findAddressByCoordinate(
        latitude: Double,
        longitude: Double,
        apiKey: String
    ): String? {
        val encodedLng = URLEncoder.encode(longitude.toString(), "UTF-8")
        val encodedLat = URLEncoder.encode(latitude.toString(), "UTF-8")
        val json = requestJson(
            urlText = "$LOCAL_API_BASE/geo/coord2address.json?x=$encodedLng&y=$encodedLat",
            apiKey = apiKey
        )

        val firstDocument = json.optJSONArray("documents")
            ?.takeIf { it.length() > 0 }
            ?.optJSONObject(0)
            ?: return null

        val roadAddress = firstDocument.optJSONObject("road_address")
            ?.optString("address_name")
            ?.takeIf { it.isNotBlank() }

        val address = firstDocument.optJSONObject("address")
            ?.optString("address_name")
            ?.takeIf { it.isNotBlank() }

        return roadAddress ?: address
    }

    private fun searchAddress(query: String, apiKey: String): SavedPlace? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val json = requestJson(
            urlText = "$LOCAL_API_BASE/search/address.json?query=$encodedQuery",
            apiKey = apiKey
        )

        val firstDocument = json.optJSONArray("documents")
            ?.takeIf { it.length() > 0 }
            ?.optJSONObject(0)
            ?: return null

        val longitude = firstDocument.optString("x").toDoubleOrNull() ?: return null
        val latitude = firstDocument.optString("y").toDoubleOrNull() ?: return null
        val roadAddress = firstDocument.optJSONObject("road_address")
            ?.optString("address_name")
            ?.takeIf { it.isNotBlank() }
        val address = firstDocument.optString("address_name")
            .takeIf { it.isNotBlank() }
            ?: query

        return SavedPlace(
            latitude = latitude,
            longitude = longitude,
            address = roadAddress ?: address
        )
    }

    private fun searchKeyword(query: String, apiKey: String): SavedPlace? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val json = requestJson(
            urlText = "$LOCAL_API_BASE/search/keyword.json?query=$encodedQuery",
            apiKey = apiKey
        )

        val firstDocument = json.optJSONArray("documents")
            ?.takeIf { it.length() > 0 }
            ?.optJSONObject(0)
            ?: return null

        val longitude = firstDocument.optString("x").toDoubleOrNull() ?: return null
        val latitude = firstDocument.optString("y").toDoubleOrNull() ?: return null
        val address = firstDocument.optString("road_address_name")
            .takeIf { it.isNotBlank() }
            ?: firstDocument.optString("address_name")
                .takeIf { it.isNotBlank() }
            ?: firstDocument.optString("place_name")
                .takeIf { it.isNotBlank() }
            ?: query

        return SavedPlace(
            latitude = latitude,
            longitude = longitude,
            address = address
        )
    }

    private fun requestJson(
        urlText: String,
        apiKey: String
    ): JSONObject {
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Authorization", "KakaoAK $apiKey")
        }

        val response = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val error = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            throw IllegalStateException("Kakao Local API ${connection.responseCode}: $error")
        }

        return JSONObject(response)
    }

    companion object {
        private const val LOCAL_API_BASE = "https://dapi.kakao.com/v2/local"
    }
}
