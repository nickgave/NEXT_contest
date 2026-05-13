package com.example.next_contest.controller

import android.graphics.Color
import android.util.Log
import com.example.next_contest.R
import com.example.next_contest.model.SavedPlace
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.Poi
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyle
import java.lang.Math.toDegrees
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class KakaoPlacePickerMapController(
    private val mapView: com.kakao.vectormap.MapView
) {
    private var kakaoMap: KakaoMap? = null
    private var selectedLabel: Label? = null
    private var radiusPolygon: Polygon? = null
    private var latestPlace: SavedPlace? = null
    private var latestRadiusMeters: Int? = null
    private var started = false
    private var onMapPicked: ((latitude: Double, longitude: Double) -> Unit)? = null

    fun start(onMapPicked: (latitude: Double, longitude: Double) -> Unit) {
        this.onMapPicked = onMapPicked

        if (started) return

        started = true
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) {
                    Log.e(TAG, "Kakao place picker map failed", error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    map.setOnMapClickListener { _, latLng, _, _ ->
                        this@KakaoPlacePickerMapController.onMapPicked
                            ?.invoke(latLng.latitude, latLng.longitude)
                    }
                    render(moveCamera = true)
                }
            }
        )
        mapView.resume()
    }

    fun stop() {
        if (!started) return

        mapView.pause()
        kakaoMap = null
        selectedLabel = null
        radiusPolygon = null
        started = false
    }

    fun showPlace(
        place: SavedPlace,
        radiusMeters: Int?,
        moveCamera: Boolean = true
    ) {
        latestPlace = place
        latestRadiusMeters = radiusMeters
        render(moveCamera)
    }

    fun updateRadius(radiusMeters: Int) {
        latestRadiusMeters = radiusMeters
        render(moveCamera = false)
    }

    private fun render(moveCamera: Boolean) {
        val map = kakaoMap ?: return
        val place = latestPlace ?: return
        val position = LatLng.from(place.latitude, place.longitude)
        val styles = LabelStyles.from(
            LabelStyle.from(R.drawable.ic_map_marker_other)
                .setTextStyles(LabelTextStyle.from(LABEL_TEXT_SIZE_PX, Color.parseColor("#2F6F73"), 2, Color.WHITE))
        )
        val labelText = LabelTextBuilder().setTexts("선택 위치")

        selectedLabel = if (selectedLabel == null) {
            map.labelManager?.layer?.addLabel(
                LabelOptions.from(position)
                    .setStyles(styles)
                    .setTexts(labelText)
            )
        } else {
            selectedLabel?.moveTo(position)
            selectedLabel?.changeStylesAndText(styles, labelText)
            selectedLabel
        }

        drawRadiusPolygon(map, position, latestRadiusMeters)

        if (moveCamera) {
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(position, DEFAULT_ZOOM)
            map.moveCamera(cameraUpdate, CameraAnimation.from(CAMERA_ANIMATION_MS, true, true))
        }
    }

    private fun drawRadiusPolygon(
        map: KakaoMap,
        center: LatLng,
        radiusMeters: Int?
    ) {
        radiusPolygon?.remove()
        radiusPolygon = null

        if (radiusMeters == null || radiusMeters <= 0) return

        val points = MapPoints.fromLatLng(makeCirclePoints(center, radiusMeters))
        val style = PolygonStyle.from(
            Color.argb(56, 47, 111, 115),
            3f,
            Color.parseColor("#2F6F73")
        )

        radiusPolygon = map.shapeManager?.layer?.addPolygon(
            PolygonOptions.from(points, style)
        )
    }

    private fun makeCirclePoints(
        center: LatLng,
        radiusMeters: Int
    ): List<LatLng> {
        val latitudeRad = Math.toRadians(center.latitude)
        val angularDistance = radiusMeters / EARTH_RADIUS_METERS
        val result = mutableListOf<LatLng>()

        for (index in 0 until CIRCLE_POINT_COUNT) {
            val bearing = 2.0 * PI * index / CIRCLE_POINT_COUNT
            val lat = center.latitude + toDegrees(angularDistance * sin(bearing))
            val lng = center.longitude +
                    toDegrees(angularDistance * cos(bearing) / cos(latitudeRad))
            result.add(LatLng.from(lat, lng))
        }

        return result
    }

    companion object {
        private const val TAG = "KakaoPlacePickerMap"
        private const val CAMERA_ANIMATION_MS = 500
        private const val DEFAULT_ZOOM = 16
        private const val LABEL_TEXT_SIZE_PX = 28
        private const val CIRCLE_POINT_COUNT = 72
        private const val EARTH_RADIUS_METERS = 6378137.0
    }
}
