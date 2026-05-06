package com.example.next_contest.controller

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.next_contest.R

class AuthController(
    private val activity: AppCompatActivity,
    private val onLoginAsPatient: () -> Unit,
    private val onLoginAsGuardian: () -> Unit
) {

    fun showLoginScreen() {
        activity.setContentView(R.layout.activity_user_auth)

        activity.findViewById<TextView>(R.id.tvAuthTitle).text = "로그인"
        activity.findViewById<Button>(R.id.btnBack).visibility = View.GONE

        activity.findViewById<Button>(R.id.btnSignUpToggle).setOnClickListener {
            showRoleSelection()
        }

        activity.findViewById<CardView>(R.id.btnLoginSubmit).setOnClickListener {
            val id = activity
                .findViewById<android.widget.EditText>(R.id.etUserId)
                .text
                .toString()

            if (id.startsWith("g")) {
                onLoginAsGuardian()
            } else {
                onLoginAsPatient()
            }
        }
    }

    private fun showRoleSelection() {
        activity.setContentView(R.layout.activity_login)

        activity.findViewById<TextView>(R.id.tvLoginTitle).text = "회원가입할 역할 선택"

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            showLoginScreen()
        }

        activity.findViewById<CardView>(R.id.btnElderly).setOnClickListener {
            showSignUpScreen("elderly")
        }

        activity.findViewById<CardView>(R.id.btnGuardian).setOnClickListener {
            showSignUpScreen("guardian")
        }
    }

    private fun showSignUpScreen(role: String) {
        activity.setContentView(R.layout.activity_signup)

        activity.findViewById<TextView>(R.id.tvRoleSubtitle).text = "역할: $role"

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            showRoleSelection()
        }

        activity.findViewById<CardView>(R.id.btnSignUpSubmit).setOnClickListener {
            showLoginScreen()
        }
    }
}