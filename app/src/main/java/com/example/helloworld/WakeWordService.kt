package com.example.helloworld

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.util.*

class WakeWordService : Service(), org.vosk.android.RecognitionListener {

    private lateinit var jarvis: JarvisTTS

    private lateinit var model: Model
    private var speechService: org.vosk.android.SpeechService? = null
    private lateinit var recognizer: Recognizer
    
    private var isProcessing = false
    private var commandRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        android.util.Log.d("JARVIS", "WakeWordService started")
        jarvis = JarvisTTS(this)
        jarvis.init()

        initCommandRecognizer()

        // Load Vosk speech model
        StorageService.unpack(
            this,
            "model",
            "model",
            { loadedModel ->
                android.util.Log.d("JARVIS", "Model loaded")
                model = loadedModel
                recognizer = Recognizer(model, 16000.0f, "[\"hello jarvis\"]")
                startVosk()
            },
            { exception ->
                android.util.Log.e("JARVIS", "Model load failed", exception)
            }
        )
    }

    private fun initCommandRecognizer() {
        mainHandler.post {
            commandRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            commandRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    android.util.Log.d("JARVIS", "Ready for command...")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    android.util.Log.e("JARVIS", "Command recognition error: $error")
                    resumeVosk()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0]
                        android.util.Log.d("JARVIS", "Command received: $command")
                        processCommand(command)
                    } else {
                        resumeVosk()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startVosk() {
        speechService = org.vosk.android.SpeechService(recognizer, 16000.0f)
        speechService?.startListening(this)
        android.util.Log.d("JARVIS", "Vosk listening started")
    }

    private fun stopVosk() {
        speechService?.stop()
        speechService = null
    }

    private fun resumeVosk() {
        isProcessing = false
        if (speechService == null) {
            startVosk()
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!isProcessing && hypothesis?.contains("hello jarvis") == true) {
            isProcessing = true
            onWakeWordDetected()
        }
    }

    override fun onResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) {}
    
    override fun onError(exception: Exception?) {
        android.util.Log.e("JARVIS", "Vosk error", exception)
        isProcessing = false
    }

    override fun onTimeout() {
        isProcessing = false
    }

    private fun onWakeWordDetected() {
        android.util.Log.d("JARVIS", "Wake word detected!")
        stopVosk()
        
        // Jarvis responds first
        jarvis.speak("Yes sir") {
            // After speaking, start listening for the actual command
            startListeningForCommand()
        }
    }

    private fun startListeningForCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        
        mainHandler.post {
            commandRecognizer?.startListening(intent)
        }
    }

    private fun processCommand(command: String) {
        // Now hit the remote API with the captured command
        jarvis.speakRemote(command) {
            // Once Jarvis finishes responding, go back to listening for the wake word
            resumeVosk()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVosk()
        mainHandler.post {
            commandRecognizer?.destroy()
        }
        jarvis.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
