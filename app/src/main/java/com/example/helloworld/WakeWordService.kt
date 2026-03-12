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
                // Changed to just "jarvis"
                recognizer = Recognizer(model, 16000.0f, "[\"jarvis\"]")
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
                    // If there's a timeout or error, we go back to Vosk (standby)
                    resumeVosk()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0].lowercase(Locale.ROOT)
                        android.util.Log.d("JARVIS", "Command received: $command")
                        
                        // Check for exit commands
                        if (command.contains("got it") || command.contains("stop") || command.contains("thank you") || command.contains("goodbye")) {
                            jarvis.speak("Of course sir. Standing by.") {
                                resumeVosk()
                            }
                        } else {
                            processCommand(command)
                        }
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
        // Updated check for just "jarvis"
        if (!isProcessing && hypothesis?.contains("jarvis") == true) {
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
        // Hit the remote API with the captured command
        jarvis.speakRemote(command) {
            // After the remote response, KEEP LISTENING for the next command
            android.util.Log.d("JARVIS", "Response finished. Listening for next command...")
            startListeningForCommand()
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
