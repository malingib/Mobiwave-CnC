package com.example.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CreatedExpense(
    val id: String,
    val organisationId: String,
    val employeeId: String,
    val merchantName: String,
    val grossAmount: Double,
    val expenseDate: String
)

class SupabaseExpenseRepository {
    private val baseUrl = com.example.BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = com.example.BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun createExpense(
        accessToken: String,
        userId: String,
        merchant: String,
        amount: Double,
        category: String,
        purpose: String,
        expenseDate: String
    ): CreatedExpense = withContext(Dispatchers.IO) {
        requireConfigured()
        require(accessToken.isNotBlank() && userId.isNotBlank()) { "Your session expired. Sign in again." }
        require(merchant.isNotBlank() && amount > 0 && purpose.isNotBlank()) {
            "Enter a merchant, positive amount, and expense purpose."
        }
        val employeeRequest = Request.Builder()
            .url("$baseUrl/rest/v1/employees?select=id,organisation_id&auth_user_id=eq.$userId&status=eq.active&limit=1")
            .header("apikey", publishableKey).header("Authorization", "Bearer $accessToken").get().build()
        val employeeArray = executeArray(employeeRequest)
        if (employeeArray.length() == 0) throw IOException("Your account is not linked to an active employee record. Ask your organisation administrator to finish onboarding.")
        val employee = employeeArray.getJSONObject(0)
        val employeeId = employee.getString("id")
        val organisationId = employee.getString("organisation_id")
        val clientId = UUID.randomUUID().toString()
        val categoryKey = when (category.lowercase()) {
            "transport", "transport & fuel", "fuel" -> "transport"
            "accommodation", "lodging" -> "accommodation"
            "meals", "food" -> "meals"
            "incentives", "incentive" -> "incentives"
            else -> "other"
        }
        val payload = JSONObject()
            .put("organisation_id", organisationId)
            .put("employee_id", employeeId)
            .put("client_submission_id", clientId)
            .put("merchant_name", merchant.trim())
            .put("category", categoryKey)
            .put("purpose", purpose.trim())
            .put("gross_amount", amount)
            .put("vat_amount", 0)
            .put("currency", "KES")
            .put("expense_date", expenseDate)
            .put("status", "submitted")
            .put("verification_status", "awaiting_verification")
            .put("submitted_by", userId)
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?select=id,organisation_id,employee_id,merchant_name,gross_amount,expense_date")
            .header("apikey", publishableKey).header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json").header("Prefer", "return=representation")
            .post(payload.toString().toRequestBody(jsonMedia)).build()
        val array = executeArray(request)
        if (array.length() != 1) throw IOException("The server did not confirm expense creation.")
        val row = array.getJSONObject(0)
        CreatedExpense(
            id = row.getString("id"),
            organisationId = row.getString("organisation_id"),
            employeeId = row.getString("employee_id"),
            merchantName = row.getString("merchant_name"),
            grossAmount = row.getDouble("gross_amount"),
            expenseDate = row.getString("expense_date")
        )
    }

    suspend fun uploadEvidence(
        resolver: ContentResolver,
        accessToken: String,
        userId: String,
        expense: CreatedExpense,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        requireConfigured()
        val mime = resolver.getType(uri)?.lowercase() ?: throw IOException("Could not identify the receipt file type.")
        if (mime !in setOf("image/jpeg", "image/png", "image/webp", "application/pdf")) {
            throw IOException("Only JPEG, PNG, WebP or PDF receipts are accepted.")
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not read the selected receipt.")
        if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024) throw IOException("Receipt must be between 1 byte and 10 MB.")
        if (!signatureMatches(mime, bytes)) throw IOException("The file contents do not match the declared receipt type.")
        val extension = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "pdf"
        }
        val fileName = "\${UUID.randomUUID()}.$extension"
        val path = "\${expense.organisationId}/\${expense.id}/$fileName"
        val uploadRequest = Request.Builder()
            .url("$baseUrl/storage/v1/object/expense-evidence/$path")
            .header("apikey", publishableKey).header("Authorization", "Bearer $accessToken")
            .header("Content-Type", mime).header("x-upsert", "false")
            .post(bytes.toRequestBody(mime.toMediaType())).build()
        executeEmpty(uploadRequest)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val metadata = JSONObject().put("organisation_id", expense.organisationId)
            .put("expense_id", expense.id).put("storage_path", path).put("mime_type", mime)
            .put("file_size_bytes", bytes.size).put("sha256", digest).put("uploaded_by", userId)
        val metadataRequest = Request.Builder().url("$baseUrl/rest/v1/evidence_files")
            .header("apikey", publishableKey).header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json").header("Prefer", "return=minimal")
            .post(metadata.toString().toRequestBody(jsonMedia)).build()
        try {
            executeEmpty(metadataRequest)
        } catch (e: Exception) {
            throw IOException("The receipt file uploaded but evidence metadata could not be saved. Contact an administrator before retrying; the orphaned object requires cleanup.", e)
        }
        path
    }

    private fun executeArray(request: Request): JSONArray {
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(errorMessage(raw, response.code))
            return JSONArray(raw)
        }
    }

    private fun executeEmpty(request: Request) {
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(errorMessage(raw, response.code))
        }
    }

    private fun errorMessage(raw: String, status: Int): String {
        val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        return json.optString("message").ifBlank {
            json.optString("details").ifBlank { "Request failed ($status). Check permissions or try again." }
        }
    }

    private fun signatureMatches(mime: String, bytes: ByteArray): Boolean = when (mime) {
        "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        "image/png" -> bytes.size >= 8 && bytes.sliceArray(0..7).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        "image/webp" -> bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        "application/pdf" -> bytes.size >= 5 && String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-"
        else -> false
    }

    private fun requireConfigured() {
        if (!baseUrl.startsWith("https://") || publishableKey.isBlank() || publishableKey.contains("REPLACE")) {
            throw IOException("Supabase app configuration is incomplete.")
        }
    }
}
