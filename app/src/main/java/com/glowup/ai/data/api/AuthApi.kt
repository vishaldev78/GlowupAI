package com.glowup.ai.data.api

import com.glowup.ai.data.model.*
import com.glowup.ai.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Auth API using raw OkHttp for simplicity (no complex response parsing needed).
 */
class AuthApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("email", email.lowercase())
            put("password", password)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${Constants.BASE_URL}${Constants.SIGNIN_ENDPOINT}")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "{}"
        parseAuthResponse(responseBody, response.code)
    }

    suspend fun signUp(name: String, email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("name", name)
            put("email", email.lowercase())
            put("password", password)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${Constants.BASE_URL}${Constants.SIGNUP_ENDPOINT}")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "{}"
        parseAuthResponse(responseBody, response.code)
    }

    suspend fun getUser(token: String): UserResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${Constants.BASE_URL}${Constants.USER_ENDPOINT}")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "{\"user\":null}"
        try {
            org.json.JSONObject(responseBody).let { obj ->
                val userObj = obj.optJSONObject("user")
                if (userObj != null) {
                    UserResponse(
                        user = User(
                            id = userObj.optString("id", ""),
                            email = userObj.optString("email", ""),
                            name = userObj.optString("name", ""),
                            avatar = userObj.optString("avatar", null),
                            provider = userObj.optString("provider", "email")
                        )
                    )
                } else {
                    UserResponse(user = null)
                }
            }
        } catch (e: Exception) {
            UserResponse(user = null)
        }
    }

    private fun parseAuthResponse(body: String, statusCode: Int): AuthResponse {
        return try {
            val json = org.json.JSONObject(body)
            if (json.optBoolean("success", false)) {
                AuthResponse(
                    success = true,
                    token = json.optString("token", ""),
                    user = json.optJSONObject("user")?.let { u ->
                        User(
                            id = u.optString("id", ""),
                            email = u.optString("email", ""),
                            name = u.optString("name", "")
                        )
                    }
                )
            } else {
                AuthResponse(
                    success = false,
                    error = json.optString("error", "Authentication failed")
                )
            }
        } catch (e: Exception) {
            AuthResponse(
                success = false,
                error = "Server error. Please try again."
            )
        }
    }
}