package com.example.next_contest.controller

import android.graphics.Color
import android.util.Log
import com.example.next_contest.R
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polyline
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.PolylineStyle

class KakaoLocationMapController(
    private val mapView: MapView
) {
    private var kakaoMap: KakaoMap? = null
    private var meLabel: Label? = null
    private var otherLabel: Label? = null
    private var connectorLine: Polyline? = null
    private var latestMe: LatLng? = null
    private var latestOther: LatLng? = null
    private var latestMeLabel: String = "나"
    private var latestOtherLabel: String = "상대방"
    private var started = false

    fun start() {
        if (started) return

        started = true
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) {
                    Log.e(TAG, "Kakao map failed", error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    render()
                }
            }
        )
        mapView.resume()
    }

    fun stop() {
        if (!started) return

        mapView.pause()
        kakaoMap = null
        meLabel = null
        otherLabel = null
        connectorLine = null
        started = false
    }

    fun onLowMemory() = Unit

    fun showLocations(
        meLat: Double?,
        meLng: Double?,
        otherLat: Double?,
        otherLng: Double?,
        meLabel: String,
        otherLabel: String
    ) {
        latestMe = if (meLat != null && meLng != null) {
            LatLng.from(meLat, meLng)
        } else {
            null
        }
        latestOther = if (otherLat != null && otherLng != null) {
            LatLng.from(otherLat, otherLng)
        } else {
            null
        }
        latestMeLabel = meLabel
        latestOtherLabel = otherLabel
        render()
    }

    private fun render() {
        val map = kakaoMap ?: return
        val me = latestMe
        val other = latestOther

        if (me == null && other == null) return

        meLabel = renderLabel(
            kakaoMap = map,
            label = meLabel,
            position = me,
            text = latestMeLabel,
            drawableRes = R.drawable.ic_map_marker_me,
            textColor = Color.parseColor("#2E7D50")
        )
        otherLabel = renderLabel(
            kakaoMap = map,
            label = otherLabel,
            position = other,
            text = latestOtherLabel,
            drawableRes = R.drawable.ic_map_marker_other,
            textColor = Color.parseColor("#2F6F73")
        )

        connectorLine?.remove()
        connectorLine = if (me != null && other != null) {
            val points = MapPoints.fromLatLng(listOf(me, other))
            val style = PolylineStyle.from(LINE_WIDTH_PX, Color.parseColor("#4A6572"))
            val options = PolylineOptions.from(points, style)
            map.shapeManager?.layer?.addPolyline(options)
        } else {
            null
        }

        moveCamera(map, me, other)
    }

    private fun renderLabel(
        kakaoMap: KakaoMap,
        label: Label?,
        position: LatLng?,
        text: String,
        drawableRes: Int,
        textColor: Int
    ): Label? {
        if (position == null) {
            label?.remove()
            return null
        }

        val styles = LabelStyles.from(
            LabelStyle.from(drawableRes)
                .setTextStyles(LabelTextStyle.from(LABEL_TEXT_SIZE_PX, textColor, 2, Color.WHITE))
        )

        if (label != null) {
            label.moveTo(position)
            label.changeStylesAndText(styles, LabelTextBuilder().setTexts(text))
            return label
        }

        return kakaoMap.labelManager?.layer?.addLabel(
            LabelOptions.from(position)
                .setStyles(styles)
                .setTexts(LabelTextBuilder().setTexts(text))
        )
    }

    private fun moveCamera(
        map: KakaoMap,
        me: LatLng?,
        other: LatLng?
    ) {
        if (me != null && other != null) {
            val cameraUpdate = CameraUpdateFactory.fitMapPoints(
                arrayOf(me, other),
                CAMERA_PADDING_PX
            )
            map.moveCamera(cameraUpdate, CameraAnimation.from(CAMERA_ANIMATION_MS, true, true))
            return
        }

        val target = me ?: other ?: return
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(target, SINGLE_LOCATION_ZOOM)
        map.moveCamera(cameraUpdate, CameraAnimation.from(CAMERA_ANIMATION_MS, true, true))
    }

    companion object {
        private const val TAG = "KakaoLocationMap"
        private const val CAMERA_ANIMATION_MS = 500
        private const val CAMERA_PADDING_PX = 120
        private const val LABEL_TEXT_SIZE_PX = 28
        private const val LINE_WIDTH_PX = 8f
        private const val SINGLE_LOCATION_ZOOM = 16
    }
}
