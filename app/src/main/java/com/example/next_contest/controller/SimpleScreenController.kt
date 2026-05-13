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
import com.example.next_contest.service.PairingStatus
import com.example.next_contest.service.PairingService
import com.example.next_contest.service.PairingViewState

class SimpleScreenController(
    private val activity: AppCompatActivity,
    private val stopNavigation: () -> Unit,
    private val getUserRole: () -> UserRole,
    private val onBackToPatientMain: () -> Unit,
    private val onBackToGuardianMain: () -> Unit,
    private val onShowHomeSetting: () -> Unit,
    private val pairedLocationMapController: PairedLocationMapController,
    private val pairingService: PairingService = PairingService()
) {

    fun showSettingsScreen() {
        stopNavigation()
        pairedLocationMapController.stop()
        activity.setContentView(R.layout.activity_settings)

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            goBackByRole()
        }

        activity.findViewById<Button>(R.id.btnOpenHomeSetting).setOnClickListener {
            onShowHomeSetting()
        }

        activity.findViewById<CardView>(R.id.btnSubmitPairing).setOnClickListener {
            val pairingButton = activity.findViewById<CardView>(R.id.btnSubmitPairing)
            val phone = activity
                .findViewById<android.widget.EditText>(R.id.etTargetPhoneNumber)
                .text
                .toString()

            setCardLoading(pairingButton, true)
            pairingService.requestPairing(
                phoneNumber = phone,
                onSuccess = {
                    activity.runOnUiThread {
                        setCardLoading(pairingButton, false)
                        Toast.makeText(
                            activity,
                            "$phone 번호 사용자에게 연결 요청을 보냈습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        refreshPairingState()
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        setCardLoading(pairingButton, false)
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        activity.findViewById<Button>(R.id.btnAcceptPairingRequest).setOnClickListener {
            val requestId = it.tag as? String ?: return@setOnClickListener

            pairingService.acceptRequest(
                requestId = requestId,
                onSuccess = {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "연결이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        refreshPairingState()
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        activity.findViewById<Button>(R.id.btnCancelPairingRequest).setOnClickListener {
            val requestId = it.tag as? String ?: return@setOnClickListener

            pairingService.cancelRequest(
                requestId = requestId,
                onSuccess = {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "연결 요청을 취소했습니다.", Toast.LENGTH_SHORT).show()
                        refreshPairingState()
                    }
                },
                onFailure = { message ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        refreshPairingState()
    }

    fun showSafeZoneScreen() {
        stopNavigation()
        pairedLocationMapController.stop()
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
        pairedLocationMapController.start()

        activity.findViewById<Button>(R.id.btnBack).setOnClickListener {
            pairedLocationMapController.stop()
            goBackByRole()
        }
    }

    fun showHelpScreen() {
        stopNavigation()
        pairedLocationMapController.stop()
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

    private fun refreshPairingState() {
        pairingService.loadPairingState(
            onSuccess = { state ->
                activity.runOnUiThread {
                    showPairingState(state)
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    activity.findViewById<TextView>(R.id.tvPairingStatus).text = message
                }
            }
        )
    }

    private fun showPairingState(state: PairingViewState) {
        val statusText = activity.findViewById<TextView>(R.id.tvPairingStatus)
        val phoneInput = activity.findViewById<android.widget.EditText>(R.id.etTargetPhoneNumber)
        val submitButton = activity.findViewById<CardView>(R.id.btnSubmitPairing)
        val acceptButton = activity.findViewById<Button>(R.id.btnAcceptPairingRequest)
        val cancelButton = activity.findViewById<Button>(R.id.btnCancelPairingRequest)

        acceptButton.tag = state.requestId
        cancelButton.tag = state.requestId

        when (state.status) {
            PairingStatus.NONE -> {
                statusText.text = "연결된 사용자가 없습니다."
                phoneInput.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
            }

            PairingStatus.CONNECTED -> {
                statusText.text = "${state.counterpartName}(${state.counterpartPhone})님과 연결되어 있습니다."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
            }

            PairingStatus.OUTGOING_REQUEST -> {
                statusText.text = "${state.counterpartPhone}에 연결을 요청중..."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "취소하기"
            }

            PairingStatus.INCOMING_REQUEST -> {
                statusText.text = "${state.counterpartName}(${state.counterpartPhone})님이 연결을 요청했습니다."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "거절하기"
            }
        }
    }

    private fun setCardLoading(card: CardView, isLoading: Boolean) {
        card.isEnabled = !isLoading
        card.isClickable = !isLoading
        card.alpha = if (isLoading) 0.6f else 1.0f
    }
}
