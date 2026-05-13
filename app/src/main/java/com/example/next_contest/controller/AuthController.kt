package com.example.next_contest.controller

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.next_contest.R
import com.example.next_contest.service.AuthService
import com.example.next_contest.service.SignUpRequest

class AuthController(
    private val activity: AppCompatActivity,
    private val onLoginAsPatient: () -> Unit,
    private val onLoginAsGuardian: () -> Unit,
    private val authService: AuthService = AuthService()
) {

    fun restoreSavedSession(onNoSession: () -> Unit) {
        authService.restoreSession(
            onSuccess = { role ->
                activity.runOnUiThread {
                    enterByRole(role)
                }
            },
            onNoSession = {
                activity.runOnUiThread {
                    onNoSession()
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    onNoSession()
                }
            }
        )
    }

    fun showLoginScreen(signOutFirst: Boolean = false) {
        if (signOutFirst) {
            authService.signOut()
        }

        activity.setContentView(R.layout.activity_user_auth)

        activity.findViewById<TextView>(R.id.tvAuthTitle).text = "로그인"
        activity.findViewById<Button>(R.id.btnBack).visibility = View.GONE

        activity.findViewById<Button>(R.id.btnSignUpToggle).setOnClickListener {
            showRoleSelection()
        }

        activity.findViewById<CardView>(R.id.btnLoginSubmit).setOnClickListener {
            val loginButton = activity.findViewById<CardView>(R.id.btnLoginSubmit)
            val email = activity.findViewById<EditText>(R.id.etUserId)
                .text
                .toString()
            val password = activity.findViewById<EditText>(R.id.etUserPassword)
                .text
                .toString()

            setButtonLoading(loginButton, true)
            authService.login(
                email = email,
                password = password,
                onSuccess = { role ->
                    activity.runOnUiThread {
                        enterByRole(role)
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onFinished = {
                    activity.runOnUiThread {
                        setButtonLoading(loginButton, false)
                    }
                }
            )
        }
    }

    private fun showRoleSelection() {
        activity.setContentView(R.layout.activity_login)

        activity.findViewById<TextView>(R.id.tvLoginTitle).text = "회원가입할 역할 선택"

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            showLoginScreen()
        }

        activity.findViewById<CardView>(R.id.btnElderly).setOnClickListener {
            showSignUpScreen(AuthService.ROLE_ELDERLY)
        }

        activity.findViewById<CardView>(R.id.btnGuardian).setOnClickListener {
            showSignUpScreen(AuthService.ROLE_GUARDIAN)
        }
    }

    private fun showSignUpScreen(role: String) {
        activity.setContentView(R.layout.activity_signup)

        val roleLabel = if (role == AuthService.ROLE_GUARDIAN) "보호자" else "어르신"
        activity.findViewById<TextView>(R.id.tvRoleSubtitle).text = "역할: $roleLabel"

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            showRoleSelection()
        }

        activity.findViewById<CardView>(R.id.btnSignUpSubmit).setOnClickListener {
            val signUpButton = activity.findViewById<CardView>(R.id.btnSignUpSubmit)
            val request = SignUpRequest(
                role = role,
                name = activity.findViewById<EditText>(R.id.etNewUserName)
                    .text
                    .toString(),
                phoneNumber = activity.findViewById<EditText>(R.id.etNewUserPhone)
                    .text
                    .toString(),
                email = activity.findViewById<EditText>(R.id.etNewUserId)
                    .text
                    .toString(),
                password = activity.findViewById<EditText>(R.id.etNewUserPassword)
                    .text
                    .toString()
            )

            setButtonLoading(signUpButton, true)
            authService.signUp(
                request = request,
                onSuccess = { signedUpRole ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        enterByRole(signedUpRole)
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onFinished = {
                    activity.runOnUiThread {
                        setButtonLoading(signUpButton, false)
                    }
                }
            )
        }
    }

    private fun setButtonLoading(button: CardView, isLoading: Boolean) {
        button.isEnabled = !isLoading
        button.isClickable = !isLoading
        button.alpha = if (isLoading) 0.6f else 1.0f
    }

    private fun enterByRole(role: String) {
        if (role.trim().lowercase() == AuthService.ROLE_GUARDIAN) {
            onLoginAsGuardian()
        } else {
            onLoginAsPatient()
        }
    }
}
