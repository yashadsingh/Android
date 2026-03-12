package com.example.helloworld

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
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
    
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val CHANNEL_ID = "JarvisServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        android.util.Log.d("JARVIS", "WakeWordService creating")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        acquireWakeLock()

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
                recognizer = Recognizer(model, 16000.0f, "[\"jarvis\"]")
                startVosk()
            },
            { exception ->
                android.util.Log.e("JARVIS", "Model load failed", exception)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis is active")
            .setContentText("Listening for 'Jarvis'...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis::WakeLock")
        wakeLock?.acquire()
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
                        val command = matches[0].lowercase(Locale.ROOT)
                        android.util.Log.d("JARVIS", "Command received: $command")
                        
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
        
        jarvis.speak("Yes sir") {
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
        jarvis.speakRemote(command) {
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
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
