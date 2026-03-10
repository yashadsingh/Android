package com.example.helloworld

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.*

class WakeWordService : Service(), RecognitionListener {

    private lateinit var model: Model
    private lateinit var speechService: SpeechService
    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()

        println("WakeWordService started")

        // Initialize Text To Speech
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // Load Vosk speech model
        StorageService.unpack(
            this,
            "model",
            "model",
            { loadedModel ->

                android.util.Log.d("JARVIS", "Model loaded")

                model = loadedModel

                val recognizer = Recognizer(
                    model,
                    16000.0f,
                    "[\"hello jarvis\"]"
                )

                speechService = SpeechService(recognizer, 16000.0f)

                speechService.startListening(this)

                android.util.Log.d("JARVIS", "Listening started")

            },
            { exception ->
                android.util.Log.e("JARVIS", "Model load failed", exception)
            }
        )

    }

    override fun onPartialResult(hypothesis: String?) {

        if (hypothesis?.contains("hello jarvis") == true) {
            onWakeWordDetected()
        }
    }

    override fun onResult(hypothesis: String?) {}

    override fun onFinalResult(hypothesis: String?) {}

    override fun onError(exception: Exception?) {}

    override fun onTimeout() {}

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

        if (::speechService.isInitialized) {
            speechService.stop()
            speechService.shutdown()
        }

        tts.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}