package com.glowup.ai.data.api

import com.glowup.ai.data.store.AuthStore
import com.glowup.ai.util.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun getAuthInterceptor(authStore: AuthStore): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            runCatching {
                val token = runBlocking { authStore.getToken() }
                if (!token.isNullOrBlank()) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
            }.getOrDefault(originalRequest).let { chain.proceed(it) }
        }
    }

    fun provideRetrofit(authStore: AuthStore): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(getAuthInterceptor(authStore))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // Long timeout for YouCam polling
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
