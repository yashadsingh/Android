package com.example.helloworld

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

data class VoiceRequest(
    val userId: String,
    val audioData: String,
    val language: String
)

interface JarvisApiService {
    @Streaming
    @POST("api/jarvis/voice")
    fun getVoiceResponse(@Body request: VoiceRequest): Call<ResponseBody>

    companion object {
        private const val BASE_URL = "https://jarvis-production-be6c.up.railway.app/" // Use 10.0.2.2 for host localhost from emulator

        fun create(): JarvisApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(JarvisApiService::class.java)
        }
    }
}
