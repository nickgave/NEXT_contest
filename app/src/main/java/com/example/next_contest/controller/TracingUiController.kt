package com.example.next_contest.controller

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.next_contest.R
import com.example.next_contest.model.PatientLocation
import com.kakao.vectormap.MapView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TracingUiController(
    private val activity: AppCompatActivity
) {
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvDirection: TextView
    private lateinit var ivDirectionArrow: ImageView
    private lateinit var layoutSosBanner: CardView
    private lateinit var tvSosMessage: TextView
    private lateinit var btnDismissSos: Button
    private lateinit var tvNoPatient: TextView
    private lateinit var layoutTrackingInfo: View
    private lateinit var mapController: KakaoLocationMapController

    fun bindViews() {
        tvOnlineStatus = activity.findViewById(R.id.tvOnlineStatus)
        tvLastUpdated = activity.findViewById(R.id.tvLastUpdated)
        tvCoordinates = activity.findViewById(R.id.tvCoordinates)
        tvDistance = activity.findViewById(R.id.tvDistance)
        tvDirection = activity.findViewById(R.id.tvDirection)
        ivDirectionArrow = activity.findViewById(R.id.ivDirectionArrow)
        layoutSosBanner = activity.findViewById(R.id.layoutSosBanner)
        tvSosMessage = activity.findViewById(R.id.tvSosMessage)
        btnDismissSos = activity.findViewById(R.id.btnDismissSos)
        tvNoPatient = activity.findViewById(R.id.tvNoPatient)
        layoutTrackingInfo = activity.findViewById(R.id.layoutTrackingInfo)
        mapController = KakaoLocationMapController(
            activity.findViewById<MapView>(R.id.liveMapView)
        )
        mapController.start()

        layoutTrackingInfo.visibility = View.GONE
        layoutSosBanner.visibility = View.GONE
        tvNoPatient.visibility = View.GONE
    }

    fun setBackClickListener(onBack: () -> Unit) {
        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBack()
        }
    }

    fun setDismissSosClickListener(onDismiss: () -> Unit) {
        btnDismissSos.setOnClickListener {
            onDismiss()
        }
    }

    fun showPatientName(name: String) {
        activity.findViewById<TextView>(R.id.tvTracingTitle).text =
            "${name} 실시간 위치 추적"
    }

    fun showNoPatientMessage(message: String) {
        layoutTrackingInfo.visibility = View.GONE
        layoutSosBanner.visibility = View.GONE
        tvNoPatient.visibility = View.VISIBLE
        tvNoPatient.text = message
    }

    fun showPatientLocation(location: PatientLocation) {
        tvNoPatient.visibility = View.GONE
        layoutTrackingInfo.visibility = View.VISIBLE

        tvOnlineStatus.text =
            if (location.isOnline) {
                "온라인 - 위치 공유 중"
            } else {
                "오프라인 - 마지막 위치"
            }

        tvOnlineStatus.setTextColor(
            ContextCompat.getColor(
                activity,
                if (location.isOnline) {
                    android.R.color.holo_green_dark
                } else {
                    android.R.color.holo_red_dark
                }
            )
        )

        val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA)

        tvLastUpdated.text =
            "마지막 수신: ${location.timestamp?.let { sdf.format(Date(it)) } ?: "-"}"

        tvCoordinates.text =
            "위도: ${"%.5f".format(location.latitude)}  경도: ${"%.5f".format(location.longitude)}"

        if (location.sos) {
            layoutSosBanner.visibility = View.VISIBLE
            tvSosMessage.text = "SOS 요청이 발생했습니다!\n어르신이 도움을 요청하고 있습니다."
        } else {
            layoutSosBanner.visibility = View.GONE
        }
    }

    fun showDistanceAndDirection(
        distanceMeters: Float,
        directionText: String,
        arrowRotation: Float
    ) {
        tvDistance.text = "거리: 약 ${distanceMeters.toInt()}m"
        tvDirection.text = directionText
        ivDirectionArrow.rotation = arrowRotation
    }

    fun showMapLocations(
        guardianLat: Double?,
        guardianLng: Double?,
        patientLat: Double?,
        patientLng: Double?
    ) {
        mapController.showLocations(
            meLat = guardianLat,
            meLng = guardianLng,
            otherLat = patientLat,
            otherLng = patientLng,
            meLabel = "나",
            otherLabel = "어르신"
        )
    }

    fun hideSosBanner() {
        layoutSosBanner.visibility = View.GONE
    }

    fun stopMap() {
        if (::mapController.isInitialized) {
            mapController.stop()
        }
    }

    fun onLowMemory() {
        if (::mapController.isInitialized) {
            mapController.onLowMemory()
        }
    }
}
