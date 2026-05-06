package com.example.next_contest.data.weather

import android.util.Log
import com.example.next_contest.R
import com.example.next_contest.model.GridXY
import com.example.next_contest.model.WeatherInfo
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class WeatherRepository {

    fun convertLatLngToGrid(lat: Double, lng: Double): GridXY {
        val re = 6371.00877
        val grid = 5.0
        val slat1 = 30.0
        val slat2 = 60.0
        val olon = 126.0
        val olat = 38.0
        val xo = 43.0
        val yo = 136.0

        val degrad = Math.PI / 180.0

        val reGrid = re / grid
        val slat1Rad = slat1 * degrad
        val slat2Rad = slat2 * degrad
        val olonRad = olon * degrad
        val olatRad = olat * degrad

        var sn = tan(Math.PI * 0.25 + slat2Rad * 0.5) /
                tan(Math.PI * 0.25 + slat1Rad * 0.5)
        sn = ln(cos(slat1Rad) / cos(slat2Rad)) / ln(sn)

        var sf = tan(Math.PI * 0.25 + slat1Rad * 0.5)
        sf = sf.pow(sn) * cos(slat1Rad) / sn

        var ro = tan(Math.PI * 0.25 + olatRad * 0.5)
        ro = reGrid * sf / ro.pow(sn)

        var ra = tan(Math.PI * 0.25 + lat * degrad * 0.5)
        ra = reGrid * sf / ra.pow(sn)

        var theta = lng * degrad - olonRad
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val nx = floor(ra * sin(theta) + xo + 0.5).toInt()
        val ny = floor(ro - ra * cos(theta) + yo + 0.5).toInt()

        return GridXY(nx, ny)
    }

    fun fetchKmaVilageForecast(nx: Int, ny: Int): WeatherInfo {
        val serviceKey = "99ace3a1c892a0e3eb4dca983ebb0cb9bac953a356b848441c211d76ba539940"

        val baseDate = getKmaBaseDate()
        val baseTime = getKmaBaseTime()
        val encodedKey = URLEncoder.encode(serviceKey, "UTF-8")

        val url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                "?serviceKey=$encodedKey" +
                "&numOfRows=1000" +
                "&pageNo=1" +
                "&dataType=JSON" +
                "&base_date=$baseDate" +
                "&base_time=$baseTime" +
                "&nx=$nx" +
                "&ny=$ny"

        val result = URL(url).readText()

        val json = JSONObject(result)
        val response = json.getJSONObject("response")
        val header = response.getJSONObject("header")
        val resultCode = header.getString("resultCode")

        if (resultCode != "00") {
            val resultMsg = header.getString("resultMsg")
            throw Exception("API 오류: $resultMsg")
        }

        val items = response
            .getJSONObject("body")
            .getJSONObject("items")
            .getJSONArray("item")

        val firstItem = items.getJSONObject(0)
        val targetDate = firstItem.getString("fcstDate")
        val targetTime = firstItem.getString("fcstTime")

        var sky: String? = null
        var pty: String? = null
        var pop: String? = null
        var tmp: String? = null
        var tmn: String? = null
        var tmx: String? = null

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)

            val fcstDate = item.getString("fcstDate")
            val fcstTime = item.getString("fcstTime")
            val category = item.getString("category")
            val value = item.getString("fcstValue")

            if (fcstDate == targetDate && fcstTime == targetTime) {
                when (category) {
                    "SKY" -> sky = value
                    "PTY" -> pty = value
                    "POP" -> pop = value
                    "TMP" -> tmp = value
                }
            }

            if (fcstDate == targetDate) {
                when (category) {
                    "TMN" -> if (tmn == null) tmn = value
                    "TMX" -> if (tmx == null) tmx = value
                }
            }
        }

        val weatherText = makeWeatherText(sky, pty, pop)
        val tempText = makeTempText(tmn, tmx).ifBlank {
            tmp?.let { "현재 예보 기온 ${it}°C" } ?: ""
        }
        val activityText = makeActivitySuggestion(sky, pty, pop, tmx)
        val weatherIconRes = getWeatherIconRes(sky, pty)

        return WeatherInfo(
            weatherText = weatherText,
            tempText = tempText,
            activityText = activityText,
            weatherIconRes = weatherIconRes
        )
    }

    private fun getKmaBaseCalendar(): Calendar {
        val calendar = Calendar.getInstance()

        // 기상청 데이터 반영 지연 방지
        calendar.add(Calendar.HOUR_OF_DAY, -2)

        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val baseHour = when {
            hour < 2 -> {
                calendar.add(Calendar.DATE, -1)
                23
            }
            hour < 5 -> 2
            hour < 8 -> 5
            hour < 11 -> 8
            hour < 14 -> 11
            hour < 17 -> 14
            hour < 20 -> 17
            hour < 23 -> 20
            else -> 23
        }

        calendar.set(Calendar.HOUR_OF_DAY, baseHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar
    }

    private fun getKmaBaseDate(): String {
        val calendar = getKmaBaseCalendar()
        return SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(calendar.time)
    }

    private fun getKmaBaseTime(): String {
        val calendar = getKmaBaseCalendar()
        return SimpleDateFormat("HHmm", Locale.KOREA).format(calendar.time)
    }

    private fun makeWeatherText(sky: String?, pty: String?, pop: String?): String {
        val rainText = when (pty) {
            "0" -> null
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "4" -> "소나기"
            else -> null
        }

        val skyText = when (sky) {
            "1" -> "맑음"
            "3" -> "구름 많음"
            "4" -> "흐림"
            else -> "날씨 정보"
        }

        return if (rainText != null) {
            if (pop != null) "$rainText, 강수확률 ${pop}%" else rainText
        } else {
            if (pop != null) "$skyText, 강수확률 ${pop}%" else skyText
        }
    }

    private fun getWeatherIconRes(sky: String?, pty: String?): Int {
        return when {
            pty == "1" || pty == "4" -> R.drawable.ic_weather_rain
            pty == "2" -> R.drawable.ic_weather_rain
            pty == "3" -> R.drawable.ic_weather_snow
            sky == "1" -> R.drawable.ic_weather_sunny
            sky == "3" -> R.drawable.ic_weather_cloudy
            sky == "4" -> R.drawable.ic_weather_overcast
            else -> R.drawable.ic_weather_cloudy
        }
    }

    private fun makeTempText(tmn: String?, tmx: String?): String {
        return when {
            tmn != null && tmx != null -> "최저 ${tmn}°C / 최고 ${tmx}°C"
            tmx != null -> "최고 ${tmx}°C"
            tmn != null -> "최저 ${tmn}°C"
            else -> ""
        }
    }

    private fun makeActivitySuggestion(
        sky: String?,
        pty: String?,
        pop: String?,
        tmx: String?
    ): String {
        val maxTemp = tmx?.toDoubleOrNull()
        val rainProbability = pop?.toIntOrNull() ?: 0

        return when {
            pty != null && pty != "0" -> listOf(
                "비나 눈이 올 수 있습니다. 외출은 조심하고 실내에서 음악 듣기나 가벼운 스트레칭을 해보세요.",
                "날씨가 좋지 않으니 실내에서 따뜻한 차를 마시며 쉬어보세요.",
                "길이 미끄러울 수 있습니다. 외출보다는 집 안에서 가벼운 체조를 해보세요."
            ).random()

            rainProbability >= 60 -> listOf(
                "비가 올 가능성이 있습니다. 우산을 챙기고, 가능하면 실내 활동을 해보세요.",
                "강수확률이 높습니다. 창밖 날씨를 확인하고 무리한 외출은 피해주세요.",
                "비가 올 수 있으니 실내에서 독서나 음악 감상을 해보세요."
            ).random()

            maxTemp != null && maxTemp >= 30 -> listOf(
                "오늘은 기온이 높습니다. 물을 자주 마시고, 더운 시간대에는 실내에서 쉬어보세요.",
                "더운 날입니다. 햇볕이 강한 시간에는 외출을 줄이고 충분히 휴식하세요.",
                "무더울 수 있습니다. 시원한 곳에서 가벼운 스트레칭을 해보세요."
            ).random()

            maxTemp != null && maxTemp <= 5 -> listOf(
                "오늘은 쌀쌀합니다. 따뜻하게 입고 집 근처를 짧게 산책해보세요.",
                "기온이 낮습니다. 외출할 때는 겉옷을 꼭 챙겨주세요.",
                "추운 날입니다. 실내에서 몸을 가볍게 움직이며 체온을 유지해보세요."
            ).random()

            sky == "1" -> listOf(
                "날씨가 맑습니다. 보호자와 함께 집 근처를 가볍게 산책해보세요.",
                "맑은 날입니다. 무리하지 않는 선에서 햇볕을 쬐어보세요.",
                "오늘은 날이 좋습니다. 가까운 곳을 천천히 걸어보세요."
            ).random()

            sky == "3" -> listOf(
                "구름이 조금 많은 날입니다. 창문을 열어 환기하거나 가벼운 실내 운동을 해보세요.",
                "구름이 많은 날입니다. 집 안 정리나 간단한 스트레칭을 해보세요.",
                "흐려지기 전 가까운 곳을 짧게 산책해보는 것도 좋습니다."
            ).random()

            sky == "4" -> listOf(
                "흐린 날입니다. 무리한 외출보다는 실내에서 독서나 음악 감상을 해보세요.",
                "오늘은 흐립니다. 집 안에서 가벼운 체조를 하며 몸을 풀어보세요.",
                "날이 흐리니 편안하게 휴식하며 좋아하는 음악을 들어보세요."
            ).random()

            else -> listOf(
                "오늘은 몸 상태를 살피며 가벼운 스트레칭이나 휴식을 취해보세요.",
                "무리하지 말고 편안한 활동으로 하루를 시작해보세요.",
                "가벼운 움직임이나 독서처럼 부담 없는 활동을 해보세요."
            ).random()
        }
    }
}