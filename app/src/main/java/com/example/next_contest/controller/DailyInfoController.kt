package com.example.next_contest.controller

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.next_contest.R
import com.example.next_contest.data.weather.WeatherRepository
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class DailyInfoController(
    private val activity: AppCompatActivity,
    private val weatherRepository: WeatherRepository,
    private val requestLocationPermission: () -> Unit,
    private val onBackToPatientMain: () -> Unit
) {

    fun show() {
        activity.setContentView(R.layout.activity_daily_info)
        updateTodayInfo()

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBackToPatientMain()
        }
    }

    fun fetchWeatherUsingCurrentPhoneLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()

            // 권한 없을 때 임시 기본값
            fetchKmaVilageForecast(60, 121)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(
                        activity,
                        "현재 위치를 가져오지 못해 기본 위치 날씨를 불러옵니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    fetchKmaVilageForecast(60, 121)
                    return@addOnSuccessListener
                }

                val grid = weatherRepository.convertLatLngToGrid(
                    location.latitude,
                    location.longitude
                )

                Log.d(
                    "KMA_GRID",
                    "lat=${location.latitude}, lng=${location.longitude}, nx=${grid.nx}, ny=${grid.ny}"
                )

                fetchKmaVilageForecast(grid.nx, grid.ny)
            }
            .addOnFailureListener { e ->
                Log.e("PHONE_LOCATION", "현재 위치 불러오기 실패", e)

                Toast.makeText(
                    activity,
                    "위치 정보를 불러오지 못해 기본 위치 날씨를 불러옵니다.",
                    Toast.LENGTH_SHORT
                ).show()

                fetchKmaVilageForecast(60, 121)
            }
    }

    private fun updateTodayInfo() {
        val date = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        val day = SimpleDateFormat("EEEE", Locale.KOREA).format(Date())

        activity.findViewById<TextView>(R.id.tvCurrentDate).text = date
        activity.findViewById<TextView>(R.id.tvCurrentDay).text = day

        activity.findViewById<TextView>(R.id.tvWeatherStatus).text = "날씨 불러오는 중..."
        activity.findViewById<TextView>(R.id.tvTemperature).text = ""
        activity.findViewById<TextView>(R.id.tvActivitySuggestion).text = "잠시만 기다려주세요."

        fetchWeatherUsingCurrentPhoneLocation()
    }

    private fun fetchKmaVilageForecast(nx: Int, ny: Int) {
        thread {
            try {
                val weatherInfo = weatherRepository.fetchKmaVilageForecast(nx, ny)

                activity.runOnUiThread {
                    activity.findViewById<TextView?>(R.id.tvWeatherStatus)?.text =
                        weatherInfo.weatherText

                    activity.findViewById<TextView?>(R.id.tvTemperature)?.text =
                        weatherInfo.tempText

                    activity.findViewById<TextView?>(R.id.tvActivitySuggestion)?.text =
                        weatherInfo.activityText

                    activity.findViewById<ImageView?>(R.id.ivWeatherIcon)
                        ?.setImageResource(weatherInfo.weatherIconRes)
                }

            } catch (e: Exception) {
                Log.e("KMA_API", "날씨 API 오류", e)

                activity.runOnUiThread {
                    activity.findViewById<TextView?>(R.id.tvWeatherStatus)?.text =
                        "날씨 정보를 불러오지 못했습니다."

                    activity.findViewById<TextView?>(R.id.tvTemperature)?.text = ""

                    activity.findViewById<TextView?>(R.id.tvActivitySuggestion)?.text =
                        "오늘은 무리하지 말고 실내에서 가볍게 스트레칭을 해보세요."
                }
            }
        }
    }
}