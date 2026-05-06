package com.example.next_contest.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class TTSHelper(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    var lastSpokenMessage: String = ""
        private set

    fun init() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS

        if (isReady) {
            tts?.language = Locale.KOREAN
        }
    }

    fun speak(message: String) {
        lastSpokenMessage = message

        if (isReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "navigation_voice"
            )
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun clearLastMessage() {
        lastSpokenMessage = ""
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}