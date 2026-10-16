package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ManualReviewItem(
    val id: String,
    val expenseId: String,
    val reasonCode: String,
    val reasonDetail: String?,
    val status: String,
    val createdAt: String
)

class SupabaseManualReviewRepository {
    private val baseUrl = com.example.BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = com.example.BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun listOpen(accessToken: String): List<ManualReviewItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/invoice_manual_review_queue?select=id,expense_id,reason_code,reason_detail,status,created_at&status=in.(pending,in_review)&order=created_at.desc")
            .header("apikey", publishableKey)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .get().build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(messageFor(raw, response.code))
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ManualReviewItem(
                    id = item.getString("id"),
                    expenseId = item.optString("expense_id"),
                    reasonCode = item.optString("reason_code"),
                    reasonDetail = item.optString("reason_detail").ifBlank { null },
                    status = item.optString("status"),
                    createdAt = item.optString("created_at")
                )
            }
        }
    }

    suspend fun resolve(accessToken: String, queueId: String, decision: String, reason: String) = withContext(Dispatchers.IO) {
        require(decision == "not_verified" || decision == "needs_review") { "Unsupported review outcome." }
        require(reason.trim().length >= 5) { "Enter a review reason of at least five characters." }
        val payload = JSONObject().put("p_queue_id", queueId).put("p_decision", decision).put("p_reason", reason.trim())
        val request = Request.Builder().url("$baseUrl/rest/v1/rpc/resolve_invoice_manual_review")
            .header("apikey", publishableKey).header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMedia)).build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(messageFor(raw, response.code))
        }
    }

    private fun messageFor(raw: String, status: Int): String {
        val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        return json.optString("message").ifBlank {
            json.optString("details").ifBlank { "Request failed ($status). Check your account permissions or try again." }
        }
    }
}
