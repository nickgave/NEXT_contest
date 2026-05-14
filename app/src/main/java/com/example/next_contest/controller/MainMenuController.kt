package com.example.next_contest.controller

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.next_contest.R

class MainMenuController(
    private val activity: AppCompatActivity,
    private val stopNavigation: () -> Unit,
    private val onNavigateHome: () -> Unit,
    private val onShowDailyInfo: () -> Unit,
    private val onShowMap: () -> Unit,
    private val onShowHelp: () -> Unit,
    private val onShowSettings: () -> Unit,
    private val onShowSafeZone: () -> Unit,
    private val onReportMissing: () -> Unit,
    private val onLogout: () -> Unit
) {

    fun showPatientMainScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_patient_main)

        activity.findViewById<CardView>(R.id.btnNavigateHome).setOnClickListener {
            onNavigateHome()
        }

        activity.findViewById<CardView>(R.id.btnTodayInfo).setOnClickListener {
            onShowDailyInfo()
        }

        activity.findViewById<CardView>(R.id.btnLocationMap).setOnClickListener {
            onShowMap()
        }

        activity.findViewById<CardView>(R.id.btnHelp).setOnClickListener {
            onShowHelp()
        }

        activity.findViewById<Button>(R.id.btnSettings).setOnClickListener {
            onShowSettings()
        }

        activity.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            onLogout()
        }
    }

    fun showGuardianMainScreen() {
        stopNavigation()
        activity.setContentView(R.layout.activity_guardian_main)

        activity.findViewById<CardView>(R.id.btnLocationMap).setOnClickListener {
            onShowMap()
        }

        activity.findViewById<CardView>(R.id.btnSafeZone).setOnClickListener {
            onShowSafeZone()
        }

        activity.findViewById<CardView>(R.id.btnReportMissing).setOnClickListener {
            onReportMissing()
        }

        activity.findViewById<Button>(R.id.btnSettings).setOnClickListener {
            onShowSettings()
        }

        activity.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            onLogout()
        }
    }
}
