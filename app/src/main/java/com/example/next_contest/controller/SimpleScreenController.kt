package com.example.next_contest.controller

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.example.next_contest.R
import com.example.next_contest.model.UserRole

class SimpleScreenController(
    private val activity: AppCompatActivity,
    private val stopNavigation: () -> Unit,
    private val getUserRole: () -> UserRole,
    private val onBackToPatientMain: () -> Unit,
    private val onBackToGuardianMain: () -> Unit
) {

    fun showSettingsScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_settings)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            goBackByRole()
        }

        activity.findViewById<CardView>(R.id.btnSubmitPairing).setOnClickListener {
            val phone = activity
                .findViewById<android.widget.EditText>(R.id.etTargetPhoneNumber)
                .text
                .toString()

            if (phone.isNotEmpty()) {
                Toast.makeText(
                    activity,
                    "$phone 번호로 연결 요청을 보냈습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                goBackByRole()
            } else {
                Toast.makeText(
                    activity,
                    "전화번호를 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun showSafeZoneScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_safe_zone)

        val radiusControl = activity.findViewById<CardView>(R.id.layoutRadiusControl)
        val radiusCircle = activity.findViewById<View>(R.id.radiusCircle)
        val tvRadiusValue = activity.findViewById<TextView>(R.id.tvRadiusValue)
        val seekBar = activity.findViewById<android.widget.SeekBar>(R.id.seekBarRadius)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBackToGuardianMain()
        }

        activity.findViewById<Button>(R.id.btnRadiusToggle).setOnClickListener {
            radiusControl.visibility =
                if (radiusControl.isVisible) View.GONE else View.VISIBLE
        }

        seekBar.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val size = 100 + (progress * 2)
                    val params = radiusCircle.layoutParams
                    params.width = size
                    params.height = size
                    radiusCircle.layoutParams = params
                    tvRadiusValue.text = "설정 반경: ${progress}m"
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )
    }

    fun showMapScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_map)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            goBackByRole()
        }
    }

    fun showHelpScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_seek_help)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBackToPatientMain()
        }
    }

    private fun goBackByRole() {
        if (getUserRole() == UserRole.GUARDIAN) {
            onBackToGuardianMain()
        } else {
            onBackToPatientMain()
        }
    }
}