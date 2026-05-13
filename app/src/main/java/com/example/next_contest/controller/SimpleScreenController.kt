package com.example.next_contest.controller

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.example.next_contest.R
import com.example.next_contest.model.SavedPlace
import com.example.next_contest.model.UserRole
import com.example.next_contest.service.PairingStatus
import com.example.next_contest.service.PairingService
import com.example.next_contest.service.PairingViewState
import com.example.next_contest.service.PlaceService

private const val POLICE_PHONE_NUMBER = "112"

class SimpleScreenController(
    private val activity: AppCompatActivity,
    private val stopNavigation: () -> Unit,
    private val getUserRole: () -> UserRole,
    private val onBackToPatientMain: () -> Unit,
    private val onBackToGuardianMain: () -> Unit,
    private val onShowHomeSetting: () -> Unit,
    private val pairedLocationMapController: PairedLocationMapController,
    private val pairingService: PairingService = PairingService(),
    private val placeService: PlaceService = PlaceService()
) {

    fun showSettingsScreen() {
        stopNavigation()
        pairedLocationMapController.stop()
        activity.setContentView(R.layout.activity_settings)
        configureHomeSettingSection()

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

        activity.findViewById<Button>(R.id.btnDisconnectPairing).setOnClickListener {
            showDisconnectConfirmDialog()
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

        activity.findViewById<Button>(R.id.btnCallCaregiver).setOnClickListener {
            callConnectedCaregiver()
        }

        activity.findViewById<Button>(R.id.btnCallPolice).setOnClickListener {
            openDialer(POLICE_PHONE_NUMBER)
        }
    }

    private fun callConnectedCaregiver() {
        pairingService.loadPairingState(
            onSuccess = { state ->
                activity.runOnUiThread {
                    if (state.status != PairingStatus.CONNECTED) {
                        Toast.makeText(activity, "연결된 보호자가 없습니다.", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    val phoneNumber = state.counterpartPhone.filter { it.isDigit() || it == '+' }

                    if (phoneNumber.isBlank()) {
                        Toast.makeText(activity, "보호자 전화번호를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    openDialer(phoneNumber)
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun configureHomeSettingSection() {
        val title = activity.findViewById<TextView>(R.id.tvHomeSettingTitle)
        val desc = activity.findViewById<TextView>(R.id.tvHomeSettingDesc)
        val currentAddress = activity.findViewById<TextView>(R.id.tvHomeSettingCurrentAddress)
        val button = activity.findViewById<Button>(R.id.btnOpenHomeSetting)

        if (getUserRole() == UserRole.GUARDIAN) {
            title.text = "어르신 집 위치 설정"
            desc.text = "연결된 어르신의 집 주소를 검색하거나 지도에서 위치를 찍어 저장합니다."
            button.text = "어르신 집 위치 설정하기"
            currentAddress.text = "현재 어르신 집 주소를 확인 중입니다."
            loadCurrentHomeAddress(
                load = { onSuccess, onFailure ->
                    placeService.loadPairedElderlyHome(onSuccess, onFailure)
                },
                prefix = "현재 어르신 집"
            )
        } else {
            title.text = "집 위치 설정"
            desc.text = "주소를 검색하거나 지도에서 위치를 찍어 집 위치를 저장합니다."
            button.text = "집 위치 설정하기"
            currentAddress.text = "현재 집 주소를 확인 중입니다."
            loadCurrentHomeAddress(
                load = { onSuccess, onFailure ->
                    placeService.loadHome(onSuccess, onFailure)
                },
                prefix = "현재 집"
            )
        }
    }

    private fun loadCurrentHomeAddress(
        load: (
            onSuccess: (SavedPlace?) -> Unit,
            onFailure: (String) -> Unit
        ) -> Unit,
        prefix: String
    ) {
        val currentAddress = activity.findViewById<TextView>(R.id.tvHomeSettingCurrentAddress)

        load(
            { place ->
                activity.runOnUiThread {
                    currentAddress.text = if (place == null) {
                        "$prefix: 아직 설정되지 않았습니다."
                    } else {
                        "$prefix: ${place.address}"
                    }
                }
            },
            { message ->
                activity.runOnUiThread {
                    currentAddress.text = "$prefix: 불러오지 못했습니다. $message"
                }
            }
        )
    }

    private fun showDisconnectConfirmDialog() {
        AlertDialog.Builder(activity)
            .setTitle("연결 해제")
            .setMessage("현재 연결된 사용자와 연결을 해제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("해제") { _, _ ->
                disconnectPairing()
            }
            .show()
    }

    private fun disconnectPairing() {
        val disconnectButton = activity.findViewById<Button>(R.id.btnDisconnectPairing)
        disconnectButton.isEnabled = false
        disconnectButton.alpha = 0.6f

        pairingService.disconnectPairing(
            onSuccess = {
                activity.runOnUiThread {
                    pairedLocationMapController.stop()
                    Toast.makeText(activity, "연결을 해제했습니다.", Toast.LENGTH_SHORT).show()
                    disconnectButton.isEnabled = true
                    disconnectButton.alpha = 1.0f
                    refreshPairingState()
                }
            },
            onFailure = { message ->
                activity.runOnUiThread {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    disconnectButton.isEnabled = true
                    disconnectButton.alpha = 1.0f
                }
            }
        )
    }

    private fun openDialer(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }

        try {
            activity.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(activity, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
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
        val disconnectButton = activity.findViewById<Button>(R.id.btnDisconnectPairing)

        acceptButton.tag = state.requestId
        cancelButton.tag = state.requestId

        when (state.status) {
            PairingStatus.NONE -> {
                statusText.text = "연결된 사용자가 없습니다."
                phoneInput.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
                disconnectButton.visibility = View.GONE
            }

            PairingStatus.CONNECTED -> {
                statusText.text = "${state.counterpartName}(${state.counterpartPhone})님과 연결되어 있습니다."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.GONE
                disconnectButton.visibility = View.VISIBLE
            }

            PairingStatus.OUTGOING_REQUEST -> {
                statusText.text = "${state.counterpartPhone}에 연결을 요청중..."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.GONE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "취소하기"
                disconnectButton.visibility = View.GONE
            }

            PairingStatus.INCOMING_REQUEST -> {
                statusText.text = "${state.counterpartName}(${state.counterpartPhone})님이 연결을 요청했습니다."
                phoneInput.visibility = View.GONE
                submitButton.visibility = View.GONE
                acceptButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
                cancelButton.text = "거절하기"
                disconnectButton.visibility = View.GONE
            }
        }
    }

    private fun setCardLoading(card: CardView, isLoading: Boolean) {
        card.isEnabled = !isLoading
        card.isClickable = !isLoading
        card.alpha = if (isLoading) 0.6f else 1.0f
    }
}
