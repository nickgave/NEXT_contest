package com.example.next_contest.util

object DirectionUtils {

    fun getPatientDirectionCaption(angle: Float): String {
        val a = ((angle % 360) + 360) % 360

        return when {
            a < 22.5 || a >= 337.5 -> "어르신이 앞쪽에 있습니다"
            a < 67.5 -> "어르신이 오른쪽 앞에 있습니다"
            a < 112.5 -> "어르신이 오른쪽에 있습니다"
            a < 157.5 -> "어르신이 오른쪽 뒤에 있습니다"
            a < 202.5 -> "어르신이 뒤쪽에 있습니다"
            a < 247.5 -> "어르신이 왼쪽 뒤에 있습니다"
            a < 292.5 -> "어르신이 왼쪽에 있습니다"
            else -> "어르신이 왼쪽 앞에 있습니다"
        }
    }
}