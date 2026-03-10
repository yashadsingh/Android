package com.example.helloworld

import ai.picovoice.porcupine.PorcupineManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.*

class WakeWordService : Service() {

    private lateinit var porcupineManager: PorcupineManager
    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()

        // Initialize Text To Speech
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // Initialize Porcupine wake word engine
        porcupineManager = PorcupineManager.Builder()
            .setAccessKey("YOUR_ACCESS_KEY")
            .setKeywordPath("hello_jarvis.ppn")
            .build(this) { keywordIndex ->
                onWakeWordDetected()
            }

        porcupineManager.start()
    }

    private fun onWakeWordDetected() {

        speak("Yes sir")

        startSpeechRecognition()
    }

    private fun startSpeechRecognition() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()

        porcupineManager.stop()
        tts.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}