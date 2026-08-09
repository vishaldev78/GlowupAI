package com.glowup.ai.data.api

import android.util.Base64
import android.util.Log
import com.glowup.ai.data.model.TryOnResponse
import com.glowup.ai.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Ultimate YouCam API Client.
 * Displays REAL errors for debugging.
 */
class YouCamApi(
    private val apiKey: String = Constants.YOUCAM_API_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json".toMediaType()
    private val IMAGE_TYPE = "image/jpeg".toMediaType()

    suspend fun tryOn(
        personBase64: String,
        garmentBase64: String,
        category: String
    ): TryOnResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("YouCamApi", "🚀 AI START")

            // 1. Upload User
            val personId = performAdvancedUpload(personBase64) 
            if (personId.fileId == null) return@withContext TryOnResponse(false, error = personId.error)

            // 2. Upload Garment
            val garmentId = performAdvancedUpload(garmentBase64)
            if (garmentId.fileId == null) return@withContext TryOnResponse(false, error = garmentId.error)

            // 3. Create Task
            val taskResult = createTask(personId.fileId, garmentId.fileId, category)
            if (taskResult.taskId == null) return@withContext TryOnResponse(false, error = taskResult.error)

            // 4. Poll
            return@withContext pollResult(taskResult.taskId)

        } catch (e: Exception) {
            TryOnResponse(false, error = "Connection Issue: ${e.localizedMessage}")
        }
    }

    private data class UploadInfo(val fileId: String? = null, val error: String? = null)
    private data class TaskInfo(val taskId: String? = null, val error: String? = null)

    private fun performAdvancedUpload(base64: String): UploadInfo {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            
            // Handshake
            val handshakeJson = JSONObject().apply {
                val fileArr = JSONArray().put(JSONObject().apply {
                    put("file_name", "upload.jpg")
                    put("file_size", bytes.size)
                    put("content_type", "image/jpeg")
                })
                put("files", fileArr)
            }

            val request = Request.Builder()
                .url("${Constants.YOUCAM_BASE_URL}/file")
                .header("Authorization", "Bearer $apiKey")
                .header("x-api-key", apiKey)
                .post(handshakeJson.toString().toRequestBody(JSON_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                Log.d("YouCamApi", "Handshake (${response.code}): $body")

                if (!response.isSuccessful) {
                    val msg = try { JSONObject(body).optString("error", "Key Error") } catch (_: Exception) { "Server ${response.code}" }
                    return UploadInfo(error = "Upload Denied: $msg")
                }

                val jsonResponse = JSONObject(body)
                val dataObj = jsonResponse.optJSONObject("data")
                val filesArr = dataObj?.optJSONArray("files")

                if (filesArr == null || filesArr.length() == 0) {
                    val errorMsg = jsonResponse.optString("error", jsonResponse.optString("message", "No 'files' in response"))
                    return UploadInfo(error = "API Error: $errorMsg")
                }

                val fileObj = filesArr.getJSONObject(0)
                val fileId = fileObj.optString("file_id")
                
                // The URL is nested inside the 'requests' array in v2.0
                val requestsArr = fileObj.optJSONArray("requests")
                val uploadUrl = if (requestsArr != null && requestsArr.length() > 0) {
                    requestsArr.getJSONObject(0).optString("url")
                } else ""

                if (fileId.isEmpty() || uploadUrl.isEmpty()) {
                    return UploadInfo(error = "API Error: Missing file_id or upload_url")
                }

                // Binary PUT
                val putReq = Request.Builder()
                    .url(uploadUrl)
                    .put(bytes.toRequestBody(IMAGE_TYPE))
                    .build()

                client.newCall(putReq).execute().use { putRes ->
                    if (putRes.isSuccessful) return UploadInfo(fileId = fileId)
                    return UploadInfo(error = "S3 Error: ${putRes.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("YouCamApi", "Upload Failed", e)
            return UploadInfo(error = "Upload Failed: ${e.message}")
        }
    }

    private fun createTask(src: String, ref: String, cat: String): TaskInfo {
        val json = JSONObject().apply {
            put("src_file_id", src)
            put("ref_file_id", ref)
            put("garment_category", cat)
        }

        val request = Request.Builder()
            .url("${Constants.YOUCAM_BASE_URL}/task/cloth")
            .header("Authorization", "Bearer $apiKey")
            .header("x-api-key", apiKey)
            .post(json.toString().toRequestBody(JSON_TYPE))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resJson = JSONObject(body)
                    val dataObj = resJson.optJSONObject("data")
                    
                    val taskId = when {
                        resJson.has("task_id") -> resJson.optString("task_id")
                        dataObj?.has("task_id") == true -> dataObj.optString("task_id")
                        else -> ""
                    }
                    
                    if (taskId.isNotEmpty()) TaskInfo(taskId = taskId)
                    else {
                        Log.e("YouCamApi", "Task ID not found in: $body")
                        TaskInfo(error = "Task ID missing in response")
                    }
                } else {
                    TaskInfo(error = "Task Failed: ${response.code}")
                }
            }
        } catch (e: Exception) { TaskInfo(error = "Task Request Error: ${e.message}") }
    }

    private suspend fun pollResult(id: String): TryOnResponse {
        repeat(40) {
            delay(4000)
            val req = Request.Builder()
                .url("${Constants.YOUCAM_BASE_URL}/task/cloth/$id")
                .header("Authorization", "Bearer $apiKey")
                .header("x-api-key", apiKey)
                .get()
                .build()

            val res = try { client.newCall(req).execute() } catch (e: Exception) { 
                Log.e("YouCamApi", "Poll network error", e)
                null 
            }
            val body = res?.body?.string() ?: ""
            Log.d("YouCamApi", "Poll Response (${res?.code}): $body")

            val json = try { JSONObject(body) } catch (e: Exception) { JSONObject() }
            
            // Aggressive Search: Check both root and 'data' object
            val dataObj = json.optJSONObject("data")
            
            val status = when {
                json.has("task_status") -> json.optString("task_status")
                dataObj?.has("task_status") == true -> dataObj.optString("task_status")
                json.has("status") -> json.optString("status")
                dataObj?.has("status") == true -> dataObj.optString("status")
                else -> ""
            }

            if (status == "success") {
                // Search for URL in results.url or result_url
                val resultsObj = json.optJSONObject("results") ?: dataObj?.optJSONObject("results")
                val resultUrl = resultsObj?.optString("url") 
                    ?: json.optString("result_url") 
                    ?: dataObj?.optString("result_url") 
                    ?: ""
                
                if (resultUrl.isNotEmpty()) {
                    Log.d("YouCamApi", "✅ AI SUCCESS: $resultUrl")
                    return TryOnResponse(true, resultImageUrl = resultUrl)
                } else {
                    Log.e("YouCamApi", "❌ Success but URL not found in: $body")
                }
            }
            
            if (status == "failed") {
                val errorMsg = (json.optString("error_message").takeIf { it.isNotEmpty() }
                    ?: dataObj?.optString("error_message")?.takeIf { it.isNotEmpty() }
                    ?: "AI Logic Error")
                return TryOnResponse(false, error = errorMsg)
            }
            
            Log.d("YouCamApi", "Task $id: Status detected as '$status'")
        }
        return TryOnResponse(false, error = "Studio Timed Out")
    }

    suspend fun downloadImageAsBase64(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(imageUrl).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    Base64.encodeToString(response.body?.bytes(), Base64.NO_WRAP)
                } else null
            }
        } catch (e: Exception) { null }
    }
}
