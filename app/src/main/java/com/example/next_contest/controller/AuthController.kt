package com.example.next_contest.controller

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.next_contest.R
import com.example.next_contest.service.ConfirmedPhoneVerification
import com.example.next_contest.service.AuthService
import com.example.next_contest.service.PhoneVerificationSession
import com.example.next_contest.service.SignUpRequest

class AuthController(
    private val activity: AppCompatActivity,
    private val onLoginAsPatient: () -> Unit,
    private val onLoginAsGuardian: () -> Unit,
    private val authService: AuthService = AuthService()
) {
    private var phoneVerificationSession: PhoneVerificationSession? = null
    private var confirmedPhoneVerification: ConfirmedPhoneVerification? = null

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
        phoneVerificationSession = null
        confirmedPhoneVerification = null

        val roleLabel = if (role == AuthService.ROLE_GUARDIAN) "보호자" else "어르신"
        activity.findViewById<TextView>(R.id.tvRoleSubtitle).text = "역할: $roleLabel"

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            showRoleSelection()
        }

        configurePhoneVerification()

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
                    .toString(),
                phoneCredential = confirmedPhoneVerification?.credential,
                verifiedNormalizedPhone = confirmedPhoneVerification?.normalizedPhone
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

    private fun configurePhoneVerification() {
        val phoneInput = activity.findViewById<EditText>(R.id.etNewUserPhone)
        val codeInput = activity.findViewById<EditText>(R.id.etPhoneVerificationCode)
        val sendButton = activity.findViewById<Button>(R.id.btnSendPhoneVerification)
        val confirmButton = activity.findViewById<Button>(R.id.btnConfirmPhoneVerification)
        val statusText = activity.findViewById<TextView>(R.id.tvPhoneVerificationStatus)

        statusText.text = "휴대폰 인증이 필요합니다."
        codeInput.visibility = View.GONE
        confirmButton.visibility = View.GONE

        phoneInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (phoneVerificationSession == null && confirmedPhoneVerification == null) return

                    phoneVerificationSession = null
                    confirmedPhoneVerification = null
                    codeInput.setText("")
                    codeInput.visibility = View.GONE
                    confirmButton.visibility = View.GONE
                    statusText.text = "전화번호가 변경되었습니다. 다시 인증해주세요."
                }
            }
        )

        sendButton.setOnClickListener {
            setButtonLoading(sendButton, true)
            authService.sendPhoneVerificationCode(
                activity = activity,
                phoneNumber = phoneInput.text.toString(),
                onCodeSent = { session ->
                    activity.runOnUiThread {
                        phoneVerificationSession = session
                        confirmedPhoneVerification = null
                        codeInput.visibility = View.VISIBLE
                        confirmButton.visibility = View.VISIBLE
                        statusText.text = "인증번호를 보냈습니다. 문자로 받은 번호를 입력해주세요."
                        setButtonLoading(sendButton, false)
                    }
                },
                onAutoVerified = { verification ->
                    activity.runOnUiThread {
                        phoneVerificationSession = null
                        confirmedPhoneVerification = verification
                        codeInput.visibility = View.GONE
                        confirmButton.visibility = View.GONE
                        statusText.text = "휴대폰 자동 인증이 완료되었습니다."
                        setButtonLoading(sendButton, false)
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        statusText.text = message
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                        setButtonLoading(sendButton, false)
                    }
                }
            )
        }

        confirmButton.setOnClickListener {
            val session = phoneVerificationSession

            if (session == null) {
                Toast.makeText(activity, "먼저 인증번호를 받아주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authService.confirmPhoneVerificationCode(
                session = session,
                code = codeInput.text.toString(),
                onSuccess = { verification ->
                    activity.runOnUiThread {
                        confirmedPhoneVerification = verification
                        statusText.text = "인증번호 확인이 완료되었습니다. 회원가입을 눌러주세요."
                        Toast.makeText(activity, "휴대폰 인증이 확인되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        statusText.text = message
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
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

    private fun setButtonLoading(button: Button, isLoading: Boolean) {
        button.isEnabled = !isLoading
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
