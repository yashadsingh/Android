package com.example.helloworld

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*

class JarvisTTS(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val apiService = JarvisApiService.create()

    fun init() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.UK
            tts?.setSpeechRate(1.3f)
            tts?.voices?.forEach { voice ->

                if (voice.name == "en-gb-x-gbd-local")
                {
                    tts?.voice = voice
                    return@forEach
                }
            }
            ready = true
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!ready) {
            onComplete?.invoke()
            return
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onComplete?.invoke()
            }
            override fun onError(utteranceId: String?) {
                onComplete?.invoke()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts")
    }

    fun speakRemote(text: String, onComplete: (() -> Unit)? = null) {
        val request = VoiceRequest(
            userId = "android_user",
            audioData = text,
            language = "en-UK"
        )

        apiService.getVoiceResponse(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        playWav(body, onComplete)
                    }
                } else {
                    android.util.Log.e("JarvisTTS", "API Error: ${response.code()}")
                    speak(text, onComplete)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                android.util.Log.e("JarvisTTS", "API Failure", t)
                speak(text, onComplete)
            }
        })
    }

    private fun playWav(body: ResponseBody, onComplete: (() -> Unit)?) {
        try {
            val tempFile = File.createTempFile("jarvis_voice", "wav", context.cacheDir)
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { output ->
                body.byteStream().copyTo(output)
            }

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisTTS", "Failed to play WAV", e)
            onComplete?.invoke()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
