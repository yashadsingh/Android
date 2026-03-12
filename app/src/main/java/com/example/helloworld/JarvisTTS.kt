package com.example.helloworld

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts")
    }

    fun speakRemote(text: String) {
        val request = VoiceRequest(
            userId = "android_user",
            audioData = text,
            language = "en-UK"
        )

        apiService.getVoiceResponse(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        playWav(body)
                    }
                } else {
                    android.util.Log.e("JarvisTTS", "API Error: ${response.code()}")
                    // Fallback to local TTS if API fails
                    speak(text)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                android.util.Log.e("JarvisTTS", "API Failure", t)
                speak(text)
            }
        })
    }

    private fun playWav(body: ResponseBody) {
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
            }
        } catch (e: Exception) {
            android.util.Log.e("JarvisTTS", "Failed to play WAV", e)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
