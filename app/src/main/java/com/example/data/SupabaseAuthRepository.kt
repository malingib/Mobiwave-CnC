package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?,
    val expiresAtEpochSeconds: Long
)

class SupabaseAuthRepository(context: Context) {
    private val baseUrl = com.example.BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = com.example.BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "mobiwave_auth_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun configured(): Boolean = baseUrl.startsWith("https://") &&
        publishableKey.isNotBlank() && !publishableKey.contains("REPLACE")

    suspend fun signIn(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        requireConfigured()
        require(email.isNotBlank() && password.isNotBlank()) { "Enter your email and password." }
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val response = request("/auth/v1/token?grant_type=password", body)
        parseSession(response).also(::save)
    }

    suspend fun restoreSession(): AuthSession? = withContext(Dispatchers.IO) {
        if (!configured()) return@withContext null
        val access = prefs.getString("access_token", null)
        val refresh = prefs.getString("refresh_token", null)
        val userId = prefs.getString("user_id", null)
        val email = prefs.getString("email", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        if (access.isNullOrBlank() || refresh.isNullOrBlank() || userId.isNullOrBlank()) return@withContext null
        if (expiresAt > System.currentTimeMillis() / 1000 + 60) {
            return@withContext AuthSession(access, refresh, userId, email, expiresAt)
        }
        try {
            request("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh))
                .let(::parseSession).also(::save)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val token = prefs.getString("access_token", null)
        if (!token.isNullOrBlank() && configured()) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/auth/v1/logout")
                    .header("apikey", publishableKey)
                    .header("Authorization", "Bearer $token")
                    .post(ByteArray(0).toRequestBody(null))
                    .build()
                http.newCall(request).execute().use { }
            } catch (_: IOException) { }
        }
        clear()
    }

    private fun request(path: String, payload: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("apikey", publishableKey)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val parsed = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            if (!response.isSuccessful) {
                val message = parsed.optString("msg").ifBlank {
                    parsed.optString("message").ifBlank {
                        parsed.optString("error_description").ifBlank { "Authentication failed (\${response.code})." }
                    }
                }
                throw IOException(message)
            }
            return parsed
        }
    }

    private fun parseSession(json: JSONObject): AuthSession {
        val user = json.optJSONObject("user") ?: throw IOException("The authentication response did not include a user.")
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        val id = user.optString("id")
        if (access.isBlank() || refresh.isBlank() || id.isBlank()) throw IOException("The authentication response was incomplete.")
        val expiry = System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600L)
        return AuthSession(access, refresh, id, user.optString("email").ifBlank { null }, expiry)
    }

    private fun save(session: AuthSession) {
        prefs.edit().putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId).putString("email", session.email)
            .putLong("expires_at", session.expiresAtEpochSeconds).apply()
    }

    private fun clear() { prefs.edit().clear().apply() }

    private fun requireConfigured() {
        if (!configured()) throw IOException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to the app secrets configuration.")
    }
}
