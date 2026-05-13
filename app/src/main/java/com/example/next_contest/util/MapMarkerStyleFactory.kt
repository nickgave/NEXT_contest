package com.example.next_contest.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import kotlin.math.roundToInt

fun createMarkerLabelStyles(
    context: Context,
    @DrawableRes drawableRes: Int,
    anchorY: Float,
    sizeDp: Int
): LabelStyles {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val drawable = AppCompatResources.getDrawable(context, drawableRes)

    drawable?.setBounds(0, 0, sizePx, sizePx)
    drawable?.draw(canvas)

    return LabelStyles.from(
        LabelStyle.from(bitmap)
            .setAnchorPoint(0.5f, anchorY)
    )
}
