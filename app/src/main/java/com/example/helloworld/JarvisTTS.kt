package com.example.helloworld

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class JarvisTTS(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    var speechRate = 0.9f   // Jarvis speaks slightly slower

    fun init() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            tts?.language = Locale.UK

            val voices = tts?.voices

//            tts?.voices?.forEach {
//                println("VOICE: ${it.name}  ${it.locale}")
//            }

            voices?.forEach { voice ->

                if (voice.name == "en-gb-x-gbd-local")
                {
                    tts?.voice = voice
                    return@forEach
                }
            }

            tts?.setSpeechRate(1.3f)

            ready = true
        }
    }

    fun speak(text: String) {

        if (!ready) return

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_tts"
        )
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}